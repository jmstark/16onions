/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package protocol;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    protected ProtocolServer(SocketAddress socketAddress,
            AsynchronousChannelGroup channelGroup) throws IOException {
        super(socketAddress, channelGroup);
    }

    @Override
    protected final void handleNewClient(AsynchronousSocketChannel channel) {
        ServerClient client;
        DisconnectHandler disconnectHandler;
        Connection connection;
        C closure;

        client = new ServerClient();
        disconnectHandler = new ClientDisconnectHandler(client);
        connection = new Connection(channel, disconnectHandler);
        closure = handleNewClient(connection);
        if (null == closure) {
            connection.disconnect();
            return;
        }
        client.shutdown = false;
    }

    private class ServerClient {
        boolean shutdown;
        C closure;

        ServerClient() {
            shutdown = true;
            closure = null;
        }
    }

    private class ClientDisconnectHandler
            extends DisconnectHandler<ServerClient> {

        public ClientDisconnectHandler(ServerClient client) {
            super(client);
        }

        @Override
        protected void handleDisconnect(ServerClient client) {
            if (client.shutdown) {
                return;
            }
            ProtocolServer.this.handleDisconnect(client.closure);
        }

    }

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
