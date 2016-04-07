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
import protocol.Message;
import protocol.MessageParserException;
import protocol.Protocol;
import protocol.Protocol.MessageType;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
class PeerMessage extends Message {

    public static PeerMessage parseMessage(ByteBuffer buf)
            throws MessageParserException {
        int size;
        MessageType type;

        size = buf.getShort();
        type = MessageType.asMessageType(buf.getShort());
        buf.limit(size);
        switch (type) {
            case GOSSIP_HELLO:
                return HelloMessage.parse(buf, size - Protocol.HEADER_LENGTH);
            default:
                assert (false);
        }
        throw new RuntimeException("Parsing failed");
    }
}
