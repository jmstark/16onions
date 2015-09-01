/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Emertat
 */
public class Server {

    private final AsynchronousServerSocketChannel serverChannel;
    private final Logger logger;
    private final AsynchronousChannelGroup threadPool;

    public Server(SocketAddress SockAddr, AsynchronousChannelGroup threadPool) throws IOException {
        this.threadPool = threadPool;
        serverChannel = AsynchronousServerSocketChannel.open(threadPool);
        serverChannel.bind(SockAddr);
        serverChannel.accept (this, new AcceptHandler());
        logger = Logger.getLogger(Server.class.getName());
    }
    
    private class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, Server> {
        @Override
        public void completed (AsynchronousSocketChannel channel, Server server) {
            new ServerClientImpl(channel);
            logger.info("A new client has connected");
        }
        
        @Override
        public void failed (Throwable ex, Server server ){
            try {
                server.stop();
            } catch (IOException ex1) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
    }

    public void stop() throws IOException {
        this.serverChannel.close();
    }

    private class ServerClientImpl extends ServerClient {        
        private final ByteBuffer buffer;
        private final ReadHandler readHandler;
        private final WriteHandler writeHandler;

        ServerClientImpl(AsynchronousSocketChannel channel) {
            this.channel = channel;
            this.buffer = ByteBuffer.allocate(512);
            this.readHandler = new ReadHandler();
            this.writeHandler = new WriteHandler();
            channel.read(buffer, this, readHandler);
        }

        @Override
        public boolean writeMessage(ByteBuffer msg) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private class ReadHandler implements CompletionHandler<Integer, ServerClientImpl> {
            
            @Override
            public void completed (Integer result, ServerClientImpl client) {
                if (result <= 0)
                {
                    client.disconnect();
                }
                buffer.flip();
                logger.log (Level.INFO, "Read: {0}", new String(buffer.array(), 0, buffer.limit()));
                client.channel.write(buffer, client, writeHandler);
            }
            
            @Override
            public void failed (Throwable ex, ServerClientImpl client) {
                client.disconnect();
            }
        }
        
        private class WriteHandler implements CompletionHandler<Integer, ServerClientImpl>{            
            
            @Override
            public void completed (Integer result, ServerClientImpl client) {
                if (result <= 0)
                {
                    client.disconnect();
                }
                if (buffer.hasRemaining())
                    client.channel.write(buffer, client, writeHandler);
                else {
                    buffer.clear();
                    client.channel.read(buffer, client, readHandler);
                }
            }
            
            @Override
            public void failed (Throwable ex, ServerClientImpl client) {
                client.disconnect();
            }
        }
        
        private void disconnect() {
            try {
                this.channel.close();
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
