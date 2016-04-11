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
import protocol.MessageHandler;
import protocol.MessageParserException;
import protocol.Protocol.MessageType;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 * @param <C>
 */
final class GossipMessageHandler extends MessageHandler<Peer> {

    GossipMessageHandler(Peer peer) {
        super(peer);
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
            throws MessageParserException {
        PeerMessage message;

        message = dispatch(buf, type);
        handleMessage(message, peer);
    }

    void handleMessage(PeerMessage message, Peer peer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
