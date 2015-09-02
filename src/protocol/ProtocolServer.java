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
public abstract class ProtocolServer extends Server implements ServerClientMessageHandler {
    public ProtocolServer(SocketAddress socketAddress, AsynchronousChannelGroup channelGroup) throws IOException {
        super(socketAddress, channelGroup);
    }

    @Override
    protected void handleNewClient(AsynchronousSocketChannel channel) {
        new ProtocolServerClient(channel, this);
    }

    @Override
    abstract public boolean handleMessage(Message message, ProtocolServerClient client);

}
