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
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
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

/**
 *
 * @author troll
 */
public class ServerTest {

    private final Logger logger;

    public ServerTest() {
        logger = Logger.getLogger(ServerTest.class.getName());
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {

    }

    @After
    public void tearDown() {
    }

    @Test
    public void testSomeMethod() throws InterruptedException, IOException, ExecutionException {
        try {
            final int cores = Runtime.getRuntime().availableProcessors();
            final ThreadFactory threadFactory = Executors.defaultThreadFactory();
            final AsynchronousChannelGroup threadPool;
            threadPool = AsynchronousChannelGroup.withFixedThreadPool(cores, threadFactory);
            final Server server = new EchoServer(new InetSocketAddress(6001), threadPool);
            server.start();
            while (!threadPool.awaitTermination(1, TimeUnit.SECONDS));
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
            public void completed (Integer result, EchoServerClient client) {
                if (result <= 0)
                {
                    client.disconnect();
                    return;
                }
                buffer.flip();
                logger.log (Level.INFO, "Read: {0}", new String(buffer.array(), 0, buffer.limit()));
                client.channel.write(buffer, client, writeHandler);
            }

            @Override
            public void failed (Throwable ex, EchoServerClient client) {
                client.disconnect();
            }
        }

        private class WriteHandler implements CompletionHandler<Integer, EchoServerClient>{

            @Override
            public void completed (Integer result, EchoServerClient client) {
                if (result <= 0)
                {
                    client.disconnect();
                    return;
                }
                if (buffer.hasRemaining())
                    client.channel.write(buffer, client, writeHandler);
                else {
                    buffer.clear();
                    client.channel.read(buffer, client, readHandler);
                }
            }

            @Override
            public void failed (Throwable ex, EchoServerClient client) {
                client.disconnect();
            }
        }

        private void disconnect() {
            try {
                logger.info("Client Disconnected");
                this.channel.close();
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private class EchoServer extends Server {

        public EchoServer(SocketAddress SockAddr, AsynchronousChannelGroup channelGroup) throws IOException {
            super(SockAddr, channelGroup);
        }

        @Override
        protected void handleClient(AsynchronousSocketChannel channel) {
            new EchoServerClient(channel);
        }
    }
}
