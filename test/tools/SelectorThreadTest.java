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
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.logging.ConsoleHandler;
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
    private Logger logger;

    private class ServerProcessor implements EventProcessor {

        private ByteBuffer buf;

        public ServerProcessor() {
            this.buf = ByteBuffer.allocate(1024);
        }

        @Override
        public boolean process(int readyOps,
                SelectableChannel channel, SelectorThread selector) {
            int nread = 0;
            SocketChannel socket = (SocketChannel) channel;
            assert (SelectionKey.OP_READ == readyOps);
            try {
                nread = socket.read(this.buf);
                this.buf.flip();
                socket.close();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
                return false;
            }
            logger.log(Level.INFO, "Read {0} bytes: {1}",
                    new Object[]{nread, new String(buf.array())});
            buf.clear();
            return false;
        }
    }

    private class ListenProcessor implements EventProcessor {

        @Override
        public boolean process(int readyOps,
                SelectableChannel channel, SelectorThread instance) {
            assert (acceptChannel.equals(channel));
            try {
                assert (SelectionKey.OP_ACCEPT == readyOps);
                SocketChannel connection = acceptChannel.accept();
                assert (null != connection);
                connection.configureBlocking(false);
                instance.addChannel(connection, SelectionKey.OP_READ, new ServerProcessor());
                return false;
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
            return false;
        }
    }

    private class ClientProcessor implements EventProcessor {
        
        private ByteBuffer buf;
        
        public ClientProcessor(){
            buf = ByteBuffer.wrap("Hello World".getBytes(StandardCharsets.US_ASCII));
        }

        @Override
        public boolean process(int readyOps, SelectableChannel channel, SelectorThread selector) {
            SocketChannel socket = (SocketChannel) channel;
            assert (SelectionKey.OP_WRITE == readyOps);
            assert (socket.isConnected());
            try {
                socket.write(buf);
                buf.rewind();
                //socket.shutdownOutput();
            } catch (IOException ex) {
                Logger.getLogger(SelectorThreadTest.class.getName()).log(Level.SEVERE, null, ex);
            }
            return false;
        }

    }

    private class ConnectProcessor implements EventProcessor {

        @Override
        public boolean process(int readyOps, SelectableChannel channel, SelectorThread selector) {
            assert (SelectionKey.OP_CONNECT == readyOps);
            SocketChannel socket = (SocketChannel) channel;
            try {
                assert (true == socket.finishConnect());
                selector.addChannel(channel, SelectionKey.OP_WRITE, new ClientProcessor());
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
                return false;
            }
            return true;
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
        if (null != this.acceptChannel)
            this.acceptChannel.close();
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
        if (!socket.connect(this.boundAddr))
            instance.addChannel(socket, SelectionKey.OP_CONNECT, new ConnectProcessor());
        else
            instance.addChannel(socket, SelectionKey.OP_WRITE, new ClientProcessor());
        instance.run();

    }

    /**
     * Test multi-threaded instance of class SelectorThread.
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     */
    @Test
    public void testMultiThreaded() throws IOException, InterruptedException {
        System.out.println("MultiThreaded");
        SelectorThread instance = new SelectorThread();
        instance.addChannel(acceptChannel, SelectionKey.OP_ACCEPT, new ListenProcessor());
        SocketChannel socket = SocketChannel.open();
        socket.configureBlocking(false);
        if (!socket.connect(this.boundAddr))
            instance.addChannel(socket, SelectionKey.OP_CONNECT, new ConnectProcessor());
        else
            instance.addChannel(socket, SelectionKey.OP_WRITE, new ClientProcessor());
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
