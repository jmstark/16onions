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
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
final class HelloMessage extends NeighboursMessage {


    private HelloMessage(Peer peer) throws MessageSizeExceededException {
        super(peer);
        this.changeMessageType(Protocol.MessageType.GOSSIP_HELLO);
    }

    /**
     * Parse the given buffer into a HelloMessage.
     *
     * @param buf the buffer to parse
     * @param size the size in the buffer to parse
     * @return the hello message
     */
    final protected static HelloMessage parse(ByteBuffer buf) throws MessageParserException {
        NeighboursMessage message;
        message = NeighboursMessage.parse(buf);
        if (message.peers.size() != 1) {
            throw new MessageParserException();
        }
        Peer peer = message.peers.pop();
        HelloMessage hello;
        try {
            hello = new HelloMessage(peer);
        } catch (MessageSizeExceededException ex) {
            throw new RuntimeException("This should not happen; please report it");
        }
        return hello;
    }

    static HelloMessage create(InetSocketAddress sock_address) {
        HelloMessage message;
        Peer peer = new Peer(sock_address);
        try {
            message = new HelloMessage(peer);
        } catch (MessageSizeExceededException ex) {
            throw new RuntimeException("This should not happen; please report it");
        }
        return message;
    }
}
