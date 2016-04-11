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

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Set;
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
final class GossipMessageHandler extends MessageHandler<Peer> {

    final private Cache cache;
    final static private Logger LOGGER = Logger.getLogger("Gossip");

    GossipMessageHandler(Peer peer, Cache cache) {
        super(peer);
        this.cache = cache;
    }

    private PeerMessage dispatch(ByteBuffer buf, MessageType type)
            throws MessageParserException {
        switch (type) {
            case GOSSIP_HELLO:
                return HelloMessage.parse(buf);
            case GOSSIP_NEIGHBORS:
                return NeighboursMessage.parse(buf);
            default:
                throw new MessageParserException("Unknown message");
        }
    }

    @Override
    public void parseMessage(ByteBuffer buf, MessageType type, Peer peer)
            throws MessageParserException, ProtocolException {
        PeerMessage message;

        message = dispatch(buf, type);
        handleMessage(message, peer);
    }

    void handleMessage(PeerMessage message, Peer peer) throws ProtocolException {
        if (message instanceof NeighboursMessage) {
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
        }
        if (message instanceof HelloMessage) {
            throw new ProtocolException("We do not expect a HelloMessage in this protocol version");
        }
        throw new ProtocolException("Unknown message");
    }
}
