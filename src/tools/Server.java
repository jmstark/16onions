/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.logging.Logger;

/**
 *
 * @author Emertat
 */
public abstract class Server {

    private final AsynchronousServerSocketChannel serverChannel;
    private final Logger logger;
    private final AsynchronousChannelGroup channelGroup;
    private final AcceptHandler acceptHandler;

    protected Server(SocketAddress SockAddr, AsynchronousChannelGroup channelGroup) throws IOException {
        this.logger = Logger.getLogger(Server.class.getName());
        this.channelGroup = channelGroup;
        this.serverChannel = AsynchronousServerSocketChannel.open(channelGroup);
        this.serverChannel.bind(SockAddr);
        this.acceptHandler = new AcceptHandler();
    }

    public void start() {
        this.serverChannel.accept(this, acceptHandler);
    }

    public void stop() throws IOException {
        this.channelGroup.shutdownNow();
        this.serverChannel.close();
    }

    /**
     * This function is called when a new connection is opened to the server.
     * @param channel the newly opened channel
     */
    abstract protected void handleNewClient(AsynchronousSocketChannel channel);

    private class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, Server> {

        @Override
        public void completed(AsynchronousSocketChannel channel, Server server) {
            handleNewClient(channel);
            logger.fine("A new client has connected");
            server.serverChannel.accept(server, server.acceptHandler);
        }

        @Override
        public void failed(Throwable ex, Server server) {
            try {
                server.stop();
            } catch (IOException ex1) {
                logger.severe(ex1.toString());
            }
        }
    }
}
