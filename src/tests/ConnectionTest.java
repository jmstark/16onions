/*
 * Copyright (C) 2016 totakura
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package tests;

import java.io.IOException;
import static java.lang.Math.max;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.junit.Assert;
import tools.MyRandom;
import tools.config.CliParser;

/**
 * Test case for socket connections.
 *
 * This test opens many connections to a given socket address. It will open the
 * doors of hell for your code. Let the games begin!
 *
 * Run this testcase as: java -cp[....] tests.ConnectionTest -s \
 * <listening address for server> -p <listening port number>
 *
 * @author totakura
 */
public class ConnectionTest {
    private static final Logger LOGGER = Logger.getLogger("ConnectionTest");
    private static int N_CONNECTIONS = 0;
    private static final int MAX_ACTIVE = 50;
    private static final Semaphore active = new Semaphore(MAX_ACTIVE);
    private static ScheduledExecutorService scheduledExecutor;
    private static AsynchronousChannelGroup clientThreadPool;

    public static void main(String[] args) throws IOException, InterruptedException {
        CliParser parser = new CliParser("ConnectionTest",
                "Test for socket connection handling");
        parser.addOption(Option.builder("p")
                .required(true)
                .hasArg(true)
                .longOpt("port")
                .desc("Port number of the listening socket")
                .argName("PORT")
                .type(Number.class)
                .build());
        parser.addOption(Option.builder("s")
                .required(true)
                .hasArg(true)
                .longOpt("address")
                .desc("Address or hostname of the listening socket")
                .argName("ADDRESS")
                .build());
        parser.addOption(Option.builder("n")
                .required()
                .hasArg()
                .longOpt("nconnects")
                .desc("Total number of connections to be opened")
                .argName("#CONNECTIONS")
                .type(Number.class)
                .build());
        CommandLine cli = parser.parse(args);
        String hostname = cli.getOptionValue("s");
        int port = 0;
        try {
            port = ((Long) cli.getParsedOptionValue("p")).intValue();
        } catch (ParseException ex) {
            throw new RuntimeException("Invalid value for port");
        }
        try {
            N_CONNECTIONS = ((Long) cli.getParsedOptionValue("n")).intValue();
        } catch (ParseException ex) {
            throw new RuntimeException("Invalid value for number of connections");
        }

        // start the test by opening as many connections as possible
        final int cores = Runtime.getRuntime().availableProcessors();
        final ThreadFactory threadFactory = Executors.defaultThreadFactory();
        clientThreadPool = AsynchronousChannelGroup.withFixedThreadPool(
                max(1, cores - 1), threadFactory);
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
        EchoClient[] clients = new EchoClient[N_CONNECTIONS];
        for (int i = 0; i < clients.length; i++) {
            clients[i] = new EchoClient(hostname, port,
                    clientThreadPool, scheduledExecutor, i);
            clients[i].connect();
        }
        active.acquire(MAX_ACTIVE);
        clientThreadPool.shutdown();
        scheduledExecutor.shutdown();
        Assert.assertTrue("clients didn't finish",
                clientThreadPool.awaitTermination(30, TimeUnit.SECONDS));
        Assert.assertTrue("clients didn't finish",
                scheduledExecutor.awaitTermination(10, TimeUnit.MILLISECONDS));
        for (EchoClient client : clients) {
            Assert.assertTrue(client.success);
        }
    }

    static private class EchoClient {

        private final String serverHostname;
        private final AsynchronousSocketChannel connection;
        private final byte[] randBytes;
        private final ByteBuffer writeBuffer;
        private final ClientWriteHandler writeHandler;
        private final ClientReadHandler readHandler;
        private final ByteBuffer readBuffer;
        private boolean success;
        private final SocketAddress remoteSocket;
        private final int clientId;
        private final ScheduledFuture future;

        private EchoClient(String serverHostname,
                int port,
                AsynchronousChannelGroup pool,
                ScheduledExecutorService scheduledExecutor,
                int clientId) throws IOException {
            this.serverHostname = serverHostname;
            this.success = false;
            this.randBytes = MyRandom.randBytes(4 * 1024);
            this.writeBuffer = ByteBuffer.wrap(randBytes);
            this.writeHandler = new ClientWriteHandler();
            this.readHandler = new ClientReadHandler();
            this.readBuffer = ByteBuffer.allocate(randBytes.length);
            this.connection = AsynchronousSocketChannel.open(pool);
            this.remoteSocket = new InetSocketAddress(InetAddress.getByName("localhost"), port);
            this.clientId = clientId;
            this.future = scheduledExecutor.schedule(new Runnable() {
                @Override
                public void run() {
                    LOGGER.fine("Disconnecting due to timeout");
                    EchoClient.this.disconnect();
                }
            }, 3, TimeUnit.SECONDS);
        }

        private void connect() {
            try {
                active.acquire();
            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
            this.connection.connect(this.remoteSocket, null, new ConnectHandler(0));
        }

        private void disconnect() {
            try {
                this.connection.close();
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING,
                        "Exception while closing connection: {0}",
                        ex.toString());
            }
            active.release();
        }

        private class ConnectHandler implements CompletionHandler<Void, Void> {

            private int retries;

            private ConnectHandler(int retries) {
                this.retries = retries;
            }

            @Override
            public void completed(Void v, Void a) {
                LOGGER.log(Level.FINE, "{0}-connected to server", clientId);
                connection.write(writeBuffer, null, writeHandler);
                connection.read(readBuffer, null, readHandler);
                EchoClient.this.success = true;
            }

            @Override
            public void failed(Throwable thrwbl, Void a) {
                if (this.retries < 3) {
                    this.retries++;
                    LOGGER.log(Level.WARNING, "{0}-connect failed; retrying", clientId);
                    connection.connect(remoteSocket, null,
                            new ConnectHandler(this.retries));
                    return;
                }
                LOGGER.log(Level.SEVERE, "{0}-Giving on attempting to connect", clientId);
                disconnect();
            }
        }

        private class ClientWriteHandler implements CompletionHandler<Integer, Void> {

            @Override
            public void completed(Integer v, Void a) {
                if (writeBuffer.hasRemaining()) {
                    LOGGER.log(Level.FINE, "{0}-Writing {1} bytes",
                            new Object[]{clientId, writeBuffer.hasRemaining()});
                    connection.write(writeBuffer, null, this);
                }
                LOGGER.log(Level.FINE, "{0}-Finished writing", clientId);
            }

            @Override
            public void failed(Throwable thrwbl, Void a) {
                LOGGER.log(Level.FINE,
                        "{0}-Writing failed, but its OK; disconnecting",
                        clientId);
                disconnect();
            }

        }

        private class ClientReadHandler implements CompletionHandler<Integer, Void> {

            @Override
            public void completed(Integer v, Void a) {
                if (readBuffer.position() < (randBytes.length - 1)) {
                    connection.read(readBuffer, null, this);
                    return;
                }
                readBuffer.flip();
                byte[] readBytes = readBuffer.array();
                success = Arrays.equals(readBytes, randBytes);
                LOGGER.log(Level.FINE, "{0}-closing connection", clientId);
                disconnect();
            }

            @Override
            public void failed(Throwable thrwbl, Void a) {
                LOGGER.log(Level.FINE,
                        "{0}-Reading failed, but it's OK; disconnecting", clientId);
                disconnect();
            }
        }
    }
}
