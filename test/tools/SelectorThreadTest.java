/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
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
public class SelectorThreadTest {

    private ServerSocketChannel acceptChannel;
    private SocketAddress boundAddr;
    private final Logger logger;

    private class ServerProcessor extends DefaultEventHandler {

        private final ByteBuffer buf;

        public ServerProcessor() {
            this.buf = ByteBuffer.allocate(1024);
        }

        @Override
        public void readHandler(SelectableChannel channel, SelectorThread selector) {
            int nread = 0;
            SocketChannel socket = (SocketChannel) channel;
            try {
                nread = socket.read(this.buf);
                this.buf.flip();
                socket.close();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
            logger.log(Level.INFO, "Read {0} bytes: {1}",
                    new Object[]{nread, new String(buf.array())});
            buf.clear();
            try {
                selector.removeChannel(channel);
            } catch (ChannelNotRegisteredException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
    }

    private class ListenProcessor extends DefaultEventHandler {

        @Override
        public void acceptHandler(SelectableChannel channel, SelectorThread selector) {
            assert (acceptChannel.equals(channel));
            try {
                SocketChannel connection = acceptChannel.accept();
                assert (null != connection);
                connection.configureBlocking(false);
                selector.addChannel(connection, SelectionKey.OP_READ, new ServerProcessor());
                selector.removeChannel(channel);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            } catch (ChannelAlreadyRegisteredException ex) {
                logger.log(Level.SEVERE, null, ex);
            } catch (ChannelNotRegisteredException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
    }

    private class ClientProcessor extends DefaultEventHandler {

        private final ByteBuffer buf;

        public ClientProcessor() {
            buf = ByteBuffer.wrap("Hello World".getBytes(StandardCharsets.US_ASCII));
        }

        @Override
        public void connectHandler(SelectableChannel channel, SelectorThread selector) {
            SocketChannel socket = (SocketChannel) channel;
            try {
                assert (true == socket.finishConnect());
                selector.modifyChannelInterestOps(channel, SelectionKey.OP_WRITE);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            } catch (ChannelNotRegisteredException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }

        @Override
        public void writeHandler(SelectableChannel channel, SelectorThread selector) {
            SocketChannel socket = (SocketChannel) channel;
            assert (socket.isConnected());
            try {
                socket.write(buf);
                buf.rewind();
            } catch (IOException ex) {
                try {
                    selector.removeChannel(channel);
                } catch (ChannelNotRegisteredException ex1) {
                    logger.log(Level.SEVERE, null, ex1);
                }
            }
        }

    }

    public SelectorThreadTest() {
        logger = Logger.getLogger(SelectorThreadTest.class.getName());
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws IOException, InterruptedException, ExecutionException {
        acceptChannel = ServerSocketChannel.open().bind(null, 1);
        acceptChannel.configureBlocking(false);
        this.boundAddr = acceptChannel.getLocalAddress();
        assert (null != this.boundAddr);
    }

    @After
    public void tearDown() throws IOException {
        if (null != this.acceptChannel) {
            this.acceptChannel.close();
        }
    }

    /**
     * Test single threaded instance of class SelectorThread.
     */
    @Test
    public void testSingleThreaded() throws Exception {
        System.out.println("SingleThreaded");
        SelectorThread instance = new SelectorThread();
        instance.addChannel(acceptChannel, SelectionKey.OP_ACCEPT, new ListenProcessor());
        SocketChannel socket = SocketChannel.open();
        socket.configureBlocking(false);
        if (!socket.connect(this.boundAddr)) {
            instance.addChannel(socket, SelectionKey.OP_CONNECT, new ClientProcessor());
        } else {
            instance.addChannel(socket, SelectionKey.OP_WRITE, new ClientProcessor());
        }
        instance.run();

    }

    /**
     * Test multi-threaded instance of class SelectorThread.
     *
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     */
    @Test
    public void testMultiThreaded() throws IOException, InterruptedException {
        System.out.println("MultiThreaded");
        SelectorThread instance = new SelectorThread();
        SocketChannel socket = SocketChannel.open();
        socket.configureBlocking(false);
        try {
            instance.addChannel(acceptChannel, SelectionKey.OP_ACCEPT, new ListenProcessor());
            if (!socket.connect(this.boundAddr)) {
                instance.addChannel(socket, SelectionKey.OP_CONNECT, new ClientProcessor());
            } else {
                instance.addChannel(socket, SelectionKey.OP_WRITE, new ClientProcessor());
            }
        } catch (ChannelAlreadyRegisteredException ex) {
            logger.log (Level.SEVERE, null, ex);
        }
        Thread instanceThread = new Thread(instance);
        instanceThread.start();
        instanceThread.join(1000);
        if (instanceThread.isAlive()) {
            fail("Instance Thread is still alive!");
            instance.wakeup();
        }
    }

    /**
     * Test of run method, of class SelectorThread.
     */
    // @Test
    public void testRun() throws IOException, InterruptedException {
        System.out.println("run");
        final SelectorThread instance = new SelectorThread();
        Thread thread = new Thread(instance);
        Thread.sleep(1000);
        instance.wakeup();
    }

}
