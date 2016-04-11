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
import protocol.ProtocolServer;

/**
 * Server for handling Gossip P2P connections.
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class GossipServer extends ProtocolServer<Peer> {

    private static final Logger LOGGER = Logger.getLogger("Gossip");
    private final Cache cache;
    private int max_peers;
    private int neighbors;

    public GossipServer(SocketAddress socketAddress,
            AsynchronousChannelGroup channelGroup,
            Cache cache,
            int max_peers) throws IOException {
        super(socketAddress, channelGroup);
        this.cache = cache;
        this.max_peers = max_peers;
        this.neighbors = 0;
    }

    @Override
    protected Peer handleNewClient(Connection connection) {
        AsynchronousSocketChannel channel;
        SocketAddress peer_address;
        Peer peer;

        if (neighbors >= max_peers) {
            return null;
        }
        channel = connection.getChannel();
        try {
            peer_address = channel.getRemoteAddress();
        } catch (IOException exp) {
            LOGGER.log(Level.WARNING, "Dropping new peer connection: {0}",
                    exp.toString());
            return null;
        }
        if (!(peer_address instanceof InetSocketAddress)) {
            LOGGER.log(Level.SEVERE,
                    "Peer connected via invalid socket address; dropping");
            return null;
        }
        peer = new Peer((InetSocketAddress) peer_address, connection);
        cache.addPeer(peer);
        connection.receive(new GossipMessageHandler(peer));
        return peer;
    }

    @Override
    protected void handleDisconnect(Peer peer) {
        if (!cache.removePeer(peer)) {
            LOGGER.severe("Removing an unknown peer? This is a bug; please report it");
        }
        LOGGER.log(Level.INFO, "Peer {0} disconnected", peer.toString());
    }

    public int getMaxPeers() {
        return max_peers;
    }

    public void setMaxPeers(int max_peers) {
        this.max_peers = max_peers;
    }
}
