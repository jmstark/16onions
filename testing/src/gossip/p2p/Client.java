/*
 * Copyright (C) 2016 Sree Harsha Totakura <sreeharsha@totakura.in>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package gossip.p2p;

import gossip.Cache;
import gossip.Peer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import protocol.Connection;
import protocol.DisconnectHandler;

/**
 * Class to initiate P2P connection to other peers.
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public final class Client {

    private final AsynchronousSocketChannel channel;
    private final Peer peer;
    private final ScheduledExecutorService scheduled_executor;
    private final InetSocketAddress listen_address;
    private final static Cache cache = Cache.getInstance();
    private final static Logger LOGGER = Logger.getLogger("Gossip");

    public Client(Peer peer,
            InetSocketAddress listen_address,
            AsynchronousChannelGroup group,
            ScheduledExecutorService scheduled_executor) throws
            IOException {
        this.channel = AsynchronousSocketChannel.open(group);
        this.peer = peer;
        this.listen_address = listen_address;
        this.scheduled_executor = scheduled_executor;
        channel.connect(peer.getAddress(), null,
                new ConnectionCompletionHandler());
    }

    private class ConnectionCompletionHandler implements
            CompletionHandler<Void, Void> {

        @Override
        public void completed(Void arg0, Void arg1) {
            Connection connection;
            PeerContext context;

            LOGGER.log(Level.FINE, "Connected to {0}", peer);
            context = new PeerContext(peer, scheduled_executor);
            connection = new Connection(channel,
                    new PeerDisconnectHandler(context));
            assert (!peer.isConnected());
            peer.setConnection(connection);
            context.sendHello(listen_address);
            connection.receive(new GossipMessageHandler(context));
        }

        @Override
        public void failed(Throwable arg0, Void arg1) {
            LOGGER.log(Level.INFO, "Connection to peer {0} failed", peer);
            try {
                channel.close();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            } finally {//Remove this peer if we fail to connect to it.
                cache.removePeer(peer);
            }
        }
    }

    private static class PeerDisconnectHandler
            extends DisconnectHandler<PeerContext> {

        public PeerDisconnectHandler(PeerContext closure) {
            super(closure);
        }

        @Override
        protected void handleDisconnect(PeerContext context) {
            Peer peer = context.getPeer();
            LOGGER.log(Level.INFO, "Peer {0} disconnected", peer);
            context.shutdown();
            cache.removePeer(peer);
        }
    }
}
