/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import util.MyRandom;
import util.Server;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author troll
 */
public class ServerTest {

    private static final int MAX_ACTIVE = 50;
    private static final Semaphore active = new Semaphore(MAX_ACTIVE);
    private final Logger clientLogger;

    public ServerTest() {
        clientLogger = Logger.getLogger("tests.tools.ServerTest.EchoClient");
    }

    @Before
    public void setUp() {

    }

    @After
    public void tearDown() {
    }

    @Test
    public void testSomeMethod() throws InterruptedException,
            ExecutionException, IOException {
        final int cores = Runtime.getRuntime().availableProcessors();
        //logger.log(Level.INFO, "Test running with {0} processor cores", cores/2);
        final ThreadFactory threadFactory = Executors.defaultThreadFactory();
        final AsynchronousChannelGroup serverChannelGroup;
        int port;
        serverChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(cores / 2, threadFactory);
        Server server = null;
        for (port = 6001; (null == server) && (port < 6999); port++) {
            try {
                server
                        = new EchoServer(
                                new InetSocketAddress(InetAddress.getByName("localhost"),
                                        port), serverChannelGroup);
            } catch (IOException e) {
                //Assume.assumeNoException(e);
                continue;
            }
            break;
        }
        Assume.assumeTrue(null != server);
        server.start();

        ExecutorService clientThreadPool = Executors.newFixedThreadPool(cores / 2);
        AsynchronousChannelGroup clientChannelGroup;
        clientChannelGroup = AsynchronousChannelGroup.withThreadPool(clientThreadPool);
        EchoClient[] clients = new EchoClient[5000];
        for (int i = 0; i < clients.length; i++) {
            clients[i] = new EchoClient(port, clientChannelGroup, i);
            clients[i].connect();
        }
        active.acquire(MAX_ACTIVE);
        clientChannelGroup.shutdown();
        assertTrue("clients didn't finish",
                clientChannelGroup.awaitTermination(30, TimeUnit.SECONDS));
        server.stop();
        serverChannelGroup.shutdown();
        assertTrue("server didn't finish",
                serverChannelGroup.awaitTermination(30, TimeUnit.SECONDS));
        for (EchoClient client : clients) {
            assertTrue(client.success);
        }
        if (!clientChannelGroup.isShutdown()) {
            clientChannelGroup.shutdownNow();
        }
        if (!serverChannelGroup.isShutdown()) {
            serverChannelGroup.shutdownNow();
        }
    }

    private class EchoServerClient {

        private final ByteBuffer buffer;
        private final ReadHandler readHandler;
        private final WriteHandler writeHandler;
        private final AsynchronousSocketChannel channel;
        private Logger logger;

        EchoServerClient(AsynchronousSocketChannel channel, int clientId) {
            this.channel = channel;
            this.buffer = ByteBuffer.allocate(1024);
            this.readHandler = new ReadHandler();
            this.writeHandler = new WriteHandler();
            this.logger = Logger.getLogger(String.format("ServerClient[{0}]", clientId));
            channel.read(buffer, this, readHandler);
        }

        private class ReadHandler implements CompletionHandler<Integer, EchoServerClient> {

            @Override
            public void completed(Integer result, EchoServerClient client) {
                if (result <= 0) {
                    client.disconnect();
                    return;
                }
                buffer.flip();
                logger.log(Level.FINER, "Read {0} bytes", buffer.remaining());
                client.channel.write(buffer, client, writeHandler);
            }

            @Override
            public void failed(Throwable ex, EchoServerClient client) {
                client.disconnect();
            }
        }

        private class WriteHandler implements CompletionHandler<Integer, EchoServerClient> {

            @Override
            public void completed(Integer result, EchoServerClient client) {
                if (result <= 0) {
                    client.disconnect();
                    return;
                }
                if (buffer.hasRemaining()) {
                    client.channel.write(buffer, client, writeHandler);
                } else {
                    buffer.clear();
                    client.channel.read(buffer, client, readHandler);
                }
            }

            @Override
            public void failed(Throwable ex, EchoServerClient client) {
                client.disconnect();
                logger.log(Level.WARNING, "server: exception while writing: {0}",
                        ex.toString());
            }
        }

        private void disconnect() {
            try {
                logger.fine("Client Disconnected");
                this.channel.close();
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private class EchoServer extends Server {

        private int clientsHandled;
        private Logger logger;

        public EchoServer(SocketAddress SockAddr, AsynchronousChannelGroup channelGroup) throws IOException {
            super(SockAddr, channelGroup);
            this.logger = Logger.getLogger("tests.tools.ServerTest.EchoServer");
        }

        @Override
        protected void handleNewClient(AsynchronousSocketChannel channel) {
            new EchoServerClient(channel, clientsHandled);
            logger.log(Level.INFO, "Clients Handled: {0}", ++clientsHandled);
        }
    }

    private class EchoClient {

        private final AsynchronousSocketChannel connection;
        private byte[] randBytes;
        private ByteBuffer writeBuffer;
        private ClientWriteHandler writeHandler;
        private ClientReadHandler readHandler;
        private ByteBuffer readBuffer;
        private boolean success;
        private SocketAddress remoteSocket;
        private Logger logger;
        private int clientId;

        private EchoClient(int port, AsynchronousChannelGroup pool, int clientId) throws IOException {
            this.success = false;
            this.randBytes = MyRandom.randBytes(4 * 1024);
            this.writeBuffer = ByteBuffer.wrap(randBytes);
            this.writeHandler = new ClientWriteHandler();
            this.readHandler = new ClientReadHandler();
            this.readBuffer = ByteBuffer.allocate(randBytes.length);
            this.connection = AsynchronousSocketChannel.open(pool);
            this.remoteSocket = new InetSocketAddress(InetAddress.getByName("localhost"), port);
            this.logger = clientLogger;
            this.clientId = clientId;
        }

        private void connect() {
            try {
                active.acquire();
            } catch (InterruptedException ex) {
                this.logger.log(Level.SEVERE, null, ex);
            }
            this.connection.connect(this.remoteSocket, null, new ConnectHandler(0));
        }

        private void disconnect() {
            try {
                this.connection.close();
            } catch (IOException ex) {
                logger.log(Level.WARNING,
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
                logger.log(Level.FINE, "{0}-connected to server", clientId);
                connection.write(writeBuffer, null, writeHandler);
                connection.read(readBuffer, null, readHandler);
            }

            @Override
            public void failed(Throwable thrwbl, Void a) {
                if (this.retries < 3) {
                    logger.log(Level.WARNING, "{0}-connect failed; retrying", clientId);
                    connection.connect(remoteSocket, null,
                            new ConnectHandler(this.retries++));
                    return;
                }
                logger.log(Level.SEVERE, "{0}-Giving on attempting to connect", clientId);
                disconnect();
            }
        }

        private class ClientWriteHandler implements CompletionHandler<Integer, Void> {

            @Override
            public void completed(Integer v, Void a) {
                if (writeBuffer.hasRemaining()) {
                    logger.log(Level.FINE, "{0}-Writing {1} bytes",
                            new Object[]{clientId, writeBuffer.hasRemaining()});
                    connection.write(writeBuffer, null, this);
                }
                logger.log(Level.FINE, "{0}-Finished writing", clientId);
            }

            @Override
            public void failed(Throwable thrwbl, Void a) {
                logger.log(Level.SEVERE, "{0}-Writing failed; disconnecting", clientId);
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
                logger.log(Level.FINE, "{0}-closing connection", clientId);
                disconnect();
            }

            @Override
            public void failed(Throwable thrwbl, Void a) {
                logger.log(Level.SEVERE, "{0}-Reading failed; disconnecting", clientId);
                disconnect();
            }
        }
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }
}
