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

import gossip.Bus;
import gossip.Cache;
import gossip.Peer;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import protocol.MessageHandler;
import protocol.MessageParserException;
import protocol.Protocol.MessageType;
import protocol.ProtocolException;

/**
 * Handler for handling Gossip P2P messages
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public final class GossipMessageHandler extends MessageHandler<PeerContext> {

    final private Cache cache;
    final static private Logger LOGGER = Logger.getLogger("Gossip");

    private enum State {
        INIT,
        HELLO_RECEIVED
    };

    private State state;

    public GossipMessageHandler(PeerContext context) {
        super(context);
        this.cache = Cache.getInstance();
        this.state = State.INIT;
    }

    /**
     * Dispatch the buffer to corresponding message parser. Add parsing hooks
     * for new message types here.
     *
     * @param buf
     * @param type
     * @return parsed peer message
     * @throws MessageParserException
     */
    private PeerMessage dispatch(ByteBuffer buf, MessageType type)
            throws MessageParserException {
        switch (type) {
            case GOSSIP_HELLO:
                return HelloMessage.parse(buf);
            case GOSSIP_NEIGHBORS:
                return NeighboursMessage.parse(buf);
            case GOSSIP_DATA:
                return DataMessage.parse(buf);
            default:
                throw new MessageParserException("Unknown message");
        }
    }

    @Override
    public void parseMessage(ByteBuffer buf,
            MessageType type,
            PeerContext context)
            throws MessageParserException, ProtocolException {
        switch (type) {
            case GOSSIP_HELLO:
                HelloMessage hello = HelloMessage.parse(buf);
                handleHello(hello, context);
                return;
            case GOSSIP_NEIGHBORS:
                NeighboursMessage nm = NeighboursMessage.parse(buf);
                handleNeighboursMessage(nm, context);
                return;
            case GOSSIP_DATA:
                DataMessage message;
                message = DataMessage.parse(buf);
                handleDataMessage(message, context);
            default:
                throw new MessageParserException("Unknown message");
        }
    }

    private void handleHello(HelloMessage hello, PeerContext context)
            throws ProtocolException {
        LOGGER.log(Level.FINE, "Received HELLO");
        Peer peer = context.getPeer();
        //HELLO is received as first message
        if (State.INIT != state) {
            LOGGER.log(Level.WARNING,
                    "Bad peer {0} sent HELLO more than once",
                    peer);
            throw new ProtocolException("HELLO sent more than once");
        }
        Peer orig;
        if (hello.peers.size() != 1) {
            throw new ProtocolException("Mismatched number of peers in Hello");
        }
        InetSocketAddress address = hello.peers.getFirst().getAddress();
        peer.setAddress(address);
        orig = cache.addPeer(peer);
        if (null == orig) {
            LOGGER.log(Level.FINE, "Adding {0} to cache", peer.toString());
        } else /**
         * peer is already in cache. This may happen when the peer is connecting
         * to use twice; we do not allow this.
         */
        if (orig.isConnected()) {
            LOGGER.log(Level.WARNING,
                    "{0} trying to connect twice when it is already connected",
                    peer.toString());
            throw new ProtocolException(
                    "Peer cannot be connect twice at the same time");
        } else {
            /**
             * Here we may have known about the orig peer from some other peer
             * but not yet connected to it. And in the mean time it has
             * connected to us. We allow this.
             */
            cache.replacePeer(orig, peer);
        }
        state = State.HELLO_RECEIVED;
        context.shareNeighbours();
        context.scheduleItemExchange();
    }

    private void handleNeighboursMessage(NeighboursMessage nm,
            PeerContext context) throws ProtocolException {
        if (State.HELLO_RECEIVED != state) {
            throw new ProtocolException("Diverting from protocol");
        }
        Iterator<Peer> iterator = nm.getPeersAsIterator();
        LOGGER.log(Level.FINER, "Received NeighboursMessage");
        while (iterator.hasNext()) {
            Peer new_peer = iterator.next();
            if (null == cache.addPeer(new_peer)) {
                LOGGER.log(Level.FINE, "Added a new peer: {0}", new_peer);
            } else {
                LOGGER.log(Level.FINE,
                        "Peer {0} already in cache; not adding",
                        new_peer.toString());
            }
        }
    }

    private void handleDataMessage(DataMessage message, PeerContext context)
            throws ProtocolException {
        if (State.HELLO_RECEIVED != state) {
            throw new ProtocolException("Diverted from protocol");
        }
        Page page = message.getPage();
        cache.addItem(page); //FIXME: make this either probabalistic or rate-limited
        Bus.getInstance().trigger(page);
    }
}
