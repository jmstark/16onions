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
package gossip;

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
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 * @param <C>
 */
final class GossipMessageHandler extends MessageHandler<PeerContext> {

    final private Cache cache;
    final static private Logger LOGGER = Logger.getLogger("Gossip");

    GossipMessageHandler(PeerContext context, Cache cache) {
        super(context);
        this.cache = cache;
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
        PeerMessage message;

        message = dispatch(buf, type);
        handleMessage(message, type, context);
    }

    void handleMessage(PeerMessage message,
            MessageType type,
            PeerContext context)
            throws ProtocolException {
        switch (type) {
            case GOSSIP_NEIGHBORS:
                NeighboursMessage nm = (NeighboursMessage) message;
                Iterator<Peer> iterator = nm.getPeersAsIterator();
                LOGGER.log(Level.FINER, "Received NeighboursMessage");
                while (iterator.hasNext()) {
                    Peer new_peer = iterator.next();
                    if (cache.addPeer(new_peer)) {
                        LOGGER.log(Level.FINE, "Added a new peer: {0}", new_peer);
                    }
                }
                return;
            case GOSSIP_HELLO:
                LOGGER.log(Level.FINE, "Received HELLO");
                HelloMessage hello = (HelloMessage) message;
                Peer peer = context.getPeer();
                if (hello.peers.size() != 1) {
                    throw new ProtocolException("Mismatched number of peers in Hello");
                }
                InetSocketAddress address = hello.peers.getFirst().getAddress();
                peer.setAddress(address);
                if (cache.addPeer(peer)) {
                    LOGGER.log(Level.FINE, "Adding {0} to cache", peer.toString());
                }
                context.shareNeighbours();
                return;
            case GOSSIP_DATA:
                //FIXME: Add this data to our local knowledge (probabilistically?)
                return;
            default:
                throw new RuntimeException("Control should not reach here; please report this as a bug");
        }
    }
}
