/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannel;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Assume;

/**
 *
 * @author troll
 */
public class ServerTest {

    private final Logger logger;

    public ServerTest() {
        logger = Logger.getLogger(ServerTest.class.getName());
        //logger.setLevel(Level.FINE);
    }

    @Before
    public void setUp() {

    }

    @After
    public void tearDown() {
    }

    @Test
    public void testSomeMethod() throws InterruptedException, ExecutionException {
        try {
            final int cores = Runtime.getRuntime().availableProcessors();
            final ThreadFactory threadFactory = Executors.defaultThreadFactory();
            final AsynchronousChannelGroup serverChannelGroup;
            int port;
            serverChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(cores, threadFactory);
            Server server = null;
            for (port = 6001; (null == server) && (port < 6999); port++) {
                try {
                    server = new EchoServer(new InetSocketAddress(port), serverChannelGroup);
                } catch (IOException e) {
                    //Assume.assumeNoException(e);
                    continue;
                }
                break;
            }
            Assume.assumeTrue(null != server);
            server.start();

            ExecutorService clientThreadPool = Executors.newFixedThreadPool(2);
            AsynchronousChannelGroup clientChannelGroup;
            clientChannelGroup = AsynchronousChannelGroup.withThreadPool(clientThreadPool);
            EchoClient[] clients = new EchoClient[4000];
            for (int i = 0; i < clients.length; i++) {
                clients[i] = new EchoClient(port, clientChannelGroup);
            }
            clientChannelGroup.shutdown();
            assertTrue("clients didn't finish",
                    clientChannelGroup.awaitTermination(300, TimeUnit.SECONDS));
            server.stop();
            assertTrue("server didn't finish",
                    serverChannelGroup.awaitTermination(1, TimeUnit.SECONDS));
            for (EchoClient client : clients) {
                assertTrue(client.success);
            }
            if (!clientChannelGroup.isShutdown()) {
                clientChannelGroup.shutdownNow();
            }
            if (!serverChannelGroup.isShutdown()) {
                serverChannelGroup.shutdownNow();
            }
        } catch (IOException ex) {
            Logger.getLogger(ServerTest.class.getName()).log(Level.SEVERE, null, ex);
            fail();
        }
    }

    private class EchoServerClient {

        private final ByteBuffer buffer;
        private final ReadHandler readHandler;
        private final WriteHandler writeHandler;
        private final AsynchronousSocketChannel channel;

        EchoServerClient(AsynchronousSocketChannel channel) {
            this.channel = channel;
            this.buffer = ByteBuffer.allocate(1024);
            this.readHandler = new ReadHandler();
            this.writeHandler = new WriteHandler();
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
                logger.log(Level.WARNING, "server: exception while writing{0}", ex.toString());
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

        public EchoServer(SocketAddress SockAddr, AsynchronousChannelGroup channelGroup) throws IOException {
            super(SockAddr, channelGroup);
        }

        @Override
        protected void handleNewClient(AsynchronousSocketChannel channel) {
            new EchoServerClient(channel);
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

        private EchoClient(int port, AsynchronousChannelGroup pool) throws IOException {
            this.success = false;
            this.randBytes = MyRandom.randBytes(4 * 1024);
            this.writeBuffer = ByteBuffer.wrap(randBytes);
            this.writeHandler = new ClientWriteHandler();
            this.readHandler = new ClientReadHandler();
            this.readBuffer = ByteBuffer.allocate(randBytes.length);
            this.connection = AsynchronousSocketChannel.open(pool);
            this.remoteSocket = new InetSocketAddress(port);
            this.connection.connect(this.remoteSocket, null, new ConnectHandler());
        }

        private class ConnectHandler implements CompletionHandler<Void, Void> {
            private int retries;
            private ConnectHandler(){this.retries = 0;}
            @Override
            public void completed(Void v, Void a) {
                connection.write(writeBuffer, null, writeHandler);
                connection.read(readBuffer, null, readHandler);
            }

            @Override
            public void failed(Throwable thrwbl, Void a) {
                if (retries < 3)
                {
                    logger.info("Connect failed; retrying");
                    connection.connect(remoteSocket, null, this);
                    retries++;
                    return;
                }
                try {
                    logger.log(Level.WARNING, "client: closing connection due to {0}", thrwbl.toString());
                    connection.close();
                } catch (IOException ex) {

                }
            }
        }

        private class ClientWriteHandler implements CompletionHandler<Integer, Void> {

            @Override
            public void completed(Integer v, Void a) {
                if (writeBuffer.hasRemaining()) {
                    connection.write(writeBuffer, null, this);
                }
            }

            @Override
            public void failed(Throwable thrwbl, Void a) {
                try {
                    logger.log(Level.WARNING, "client: closing connection due to {0}", thrwbl.toString());
                    connection.close();
                } catch (IOException ex) {

                }
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
                logger.fine("client: closing connection");
                try {
                    connection.close();
                } catch (IOException ex) {
                    Logger.getLogger(ServerTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            @Override
            public void failed(Throwable thrwbl, Void a) {
                try {
                    logger.log(Level.WARNING, "client: closing connection due to {0}", thrwbl.toString());
                    connection.close();
                } catch (IOException ex) {

                }
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
