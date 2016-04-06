/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gossip;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import protocol.Connection;
import protocol.Message;
import protocol.ProtocolServer;

/**
 * Server for handling Gossip P2P connections.
 * @author totakura
 */
public class GossipServer extends ProtocolServer {

    private Logger logger;
    private LinkedList<Peer> peers;

    public GossipServer(SocketAddress socketAddress, AsynchronousChannelGroup channelGroup) throws IOException {
        super(socketAddress, channelGroup);
        this.logger = Logger.getLogger("Gossip");
    }

    @Override
    protected void handleNewClient(AsynchronousSocketChannel channel) {
        SocketAddress peer_address;
        Peer peer;
        try {
            peer_address = channel.getRemoteAddress();
        } catch (IOException exp) {
            logger.log(Level.WARNING, "Dropping new peer connection: {1}",
                    exp.toString());
            closeChannelIgnoringException(channel);
            return;
        }
        if (!(peer_address instanceof InetSocketAddress)) {
            logger.log(Level.SEVERE,
                    "Peer connected via invalid socket address; dropping");
            closeChannelIgnoringException(channel);
            return;
        }
        peer = new Peer((InetSocketAddress) peer_address);
        this.peers.add(peer);
        super.handleNewClient(channel);
    }

    @Override
    protected boolean handleMessage(Message message, Connection connection) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private static void closeChannelIgnoringException(AsynchronousSocketChannel channel) {
        try {
                channel.close();
            } catch (IOException ex) {/*ignore*/}
    }
}
