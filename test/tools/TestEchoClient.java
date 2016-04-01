/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.logging.Logger;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import static org.junit.Assert.assertTrue;
import org.junit.Assume;

/**
 *
 * @author totakura
 */
public class TestEchoClient {
    
    private static final int MAX_ACTIVE = 50;
    private static final Semaphore active = new Semaphore(MAX_ACTIVE);
    private static ScheduledExecutorService scheduledExecutor;
    private AsynchronousSocketChannel connection;
    private byte[] randBytes;
    private ByteBuffer writeBuffer;
    private ClientWriteHandler writeHandler;
    private ClientReadHandler readHandler;
    private ByteBuffer readBuffer;
    private boolean success;
    private SocketAddress remoteSocket;
    private Logger logger;
    private int clientId;
    private ScheduledFuture connectFuture;
    
    private TestEchoClient(int port, AsynchronousChannelGroup pool, int clientId) throws IOException {
        this.success = false;
        this.randBytes = MyRandom.randBytes(4 * 1024);
        this.writeBuffer = ByteBuffer.wrap(randBytes);
        this.writeHandler = new ClientWriteHandler();
        this.readHandler = new ClientReadHandler();
        this.readBuffer = ByteBuffer.allocate(randBytes.length);
        this.connection = AsynchronousSocketChannel.open(pool);
        this.remoteSocket = new InetSocketAddress(InetAddress.getByName("localhost"), port);        
        this.logger = Logger.getLogger("tests.tools.TestEchoClient");
        this.clientId = clientId;        
    }
    
    private void connect() {
        try {
            active.acquire();
        } catch (InterruptedException ex) {
            Logger.getLogger(TestEchoClient.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        this.connectFuture = scheduledExecutor.schedule(new ConnectCommand(0),
                0, TimeUnit.SECONDS);
    }
    
    private void disconnect(){
        if ((null != this.connectFuture) && (!this.connectFuture.isDone())) {
            this.connectFuture.cancel(true);
            this.connectFuture = null;
        }
        try {
            this.connection.close();
        } catch (IOException ex) {
            logger.log(Level.WARNING,
                        "Exception while closing connection: {0}",
                        ex.toString());
        }
        active.release();
    }
    
    private class ConnectCommand implements Runnable {
        private int retries;
        private ConnectCommand(int retries) {
            this.retries = retries;
        }

        @Override
        public void run() {
            connection.connect(remoteSocket, null, new ConnectHandler(retries));
        }
        
    }
    
    private class ConnectHandler implements CompletionHandler<Void, Void> {
        
        private int retries;
        
        private ConnectHandler(int retries) {
            this.retries = retries;
        }
        
        @Override
        public void completed(Void v, Void a) {
            connectFuture = null;
            logger.log(Level.FINE, "{0}-connected to server", clientId);
            connection.write(writeBuffer, null, writeHandler);
            connection.read(readBuffer, null, readHandler);
        }
        
        @Override
        public void failed(Throwable thrwbl, Void a) {
            if (this.retries < 3) {
                this.retries++;
                logger.log(Level.WARNING, "{0}-connect failed; retrying", clientId);
                connectFuture =
                        scheduledExecutor.schedule(new ConnectCommand(this.retries),
                                (int) Math.pow(2,this.retries), TimeUnit.SECONDS);
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
            logger.log(Level.SEVERE, "{0}-Writing failed", clientId);
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
            logger.log(Level.SEVERE, "{0}-Reading failed", clientId);
            disconnect();
        }
    }
    
    public static void main(String[] args) throws IOException,
            InterruptedException {
        final int cores = Runtime.getRuntime().availableProcessors();
        //logger.log(Level.INFO, "Test running with {0} processor cores", cores/2);            
        int port = 6001;
        int result = 0;
        
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        ExecutorService clientThreadPool = Executors.newCachedThreadPool();
        AsynchronousChannelGroup clientChannelGroup;
        clientChannelGroup = AsynchronousChannelGroup.withThreadPool(clientThreadPool);
        TestEchoClient[] clients = new TestEchoClient[100000];
        for (int i = 0; i < clients.length; i++) {            
            clients[i] = new TestEchoClient(port, clientChannelGroup, i);
            clients[i].connect();            
        }      
        active.acquire(MAX_ACTIVE);        
        clientChannelGroup.shutdown();
        assertTrue("clients didn't finish",
                clientChannelGroup.awaitTermination(10, TimeUnit.SECONDS));
        for (TestEchoClient client: clients) {
            if (client.success) {
                continue;                
            }
            System.out.println("Clients did not finish successfully");
            result = 1;
            break;            
        }
        if (!clientChannelGroup.isShutdown()) {
            clientChannelGroup.shutdownNow();
        }
        System.exit(result);
    }
}
