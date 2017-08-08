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
package onion.api;

import java.nio.ByteBuffer;
import java.util.Arrays;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import protocol.Message;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
@EqualsAndHashCode(callSuper = true)
public class OnionTunnelIncomingMessage extends OnionApiMessage {

    @Getter private final long tunnelID;

    public OnionTunnelIncomingMessage(long tunnelID) {
        this.addHeader(Protocol.MessageType.API_ONION_TUNNEL_INCOMING);
        this.tunnelID = tunnelID;
        this.size += 4; // tunnel ID
    }

    @Override
    public void send(ByteBuffer out) {
        super.send(out);
        out.putInt((int) tunnelID);
    }

    public static OnionTunnelIncomingMessage parser(ByteBuffer buffer) throws
            MessageParserException {
        long id;
        if (buffer.remaining() != 4) {
            throw new MessageParserException("Incorrect message size");
        }
        id = Message.unsignedLongFromInt(buffer.getInt());
        return new OnionTunnelIncomingMessage(id);
    }
}
