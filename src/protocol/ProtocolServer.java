/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package protocol;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import tools.Server;

/**
 * A server which can communicate in protocol messages. The server is able to
 * parse and send protocol messages on the connections. The @a handleMessage()
 * callback is called upon every parsed message. Use the @a connection parameter
 * of this function to send a message.
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 * @param <C> Type of the closure
 */
public abstract class ProtocolServer<C> extends Server {

    public ProtocolServer(SocketAddress socketAddress,
            AsynchronousChannelGroup channelGroup) throws IOException {
        super(socketAddress, channelGroup);
    }

    @Override
    protected final void handleNewClient(AsynchronousSocketChannel channel) {
        Connection connection;
        C closure;
        ServerMessageHandler msgHandler;

        connection = new Connection(channel);
        closure = handleNewClient(connection);
        if (null == closure) {
            connection.disconnect();
            return;
        }
        connection.setDisconnectHandler(new ClientDisconnectHandler(closure));
        msgHandler = new ServerMessageHandler(closure);
        connection.receive(msgHandler);
    }

    private class ServerMessageHandler extends MessageHandler<C, Boolean> {

        private ServerMessageHandler(C closure) {
            super(closure);
        }

        @Override
        protected Boolean handleMessage(Message message, C closure) {
            return ProtocolServer.this.handleMessage(message, closure);
        }
    }

    private class ClientDisconnectHandler extends DisconnectHandler<C> {

        private ClientDisconnectHandler(C closure) {
            super(closure);
        }

        @Override
        protected void handleDisconnect(C closure) {
            ProtocolServer.this.handleDisconnect(closure);
        }

    }

    protected abstract boolean handleMessage(Message message,
            C closure);

    /**
     * Override this function for handling a new connection and associating a
     * closure to it.
     *
     * @param connection the new connection
     * @return the closure object to be associated or null to close the
     * connection
     */
    protected abstract C handleNewClient(Connection connection);

    /**
     * Override this function for handling connection disconnects.
     *
     * @param closure
     */
    protected abstract void handleDisconnect(C closure);
}
