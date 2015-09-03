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
 *
 * @author totakura
 */
public abstract class ProtocolServer extends Server {
    public ProtocolServer(SocketAddress socketAddress, AsynchronousChannelGroup channelGroup) throws IOException {
        super(socketAddress, channelGroup);
    }

    @Override
    protected void handleNewClient(AsynchronousSocketChannel channel) {
        Connection connection = new Connection(channel);
        ServerMessageHandler msgHandler = new ServerMessageHandler(connection);
        connection.receive(msgHandler);
    }

    private class ServerMessageHandler extends MessageHandler<Connection, Boolean> {

        private ServerMessageHandler(Connection connection) {
            super(connection);
        }
        @Override
        protected Boolean handleMessage(Message message, Connection connection)
        {
            return ProtocolServer.this.handleMessage(message, connection);
        }
    }

    protected abstract boolean handleMessage(Message message, Connection connection);
}
