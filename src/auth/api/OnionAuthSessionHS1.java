/*
 * Copyright (C) 2016 totakura
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
package auth.api;

import java.nio.ByteBuffer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;

/**
 *
 * @author totakura
 */
@EqualsAndHashCode(callSuper = true)
public class OnionAuthSessionHS1 extends OnionAuthApiMessage {

    @Getter protected int sessionID;
    @Getter protected byte[] payload;
    @Getter protected long requestID;

    public OnionAuthSessionHS1(int sessionID, long requestID, byte[] payload) throws MessageSizeExceededException {
        assert (sessionID <= ((1L << 32) - 1));
        this.addHeader(Protocol.MessageType.API_AUTH_SESSION_HS1);
        this.sessionID = sessionID;
        this.size += 4;
        if ((this.size + payload.length) > Protocol.MAX_MESSAGE_SIZE) {
            throw new MessageSizeExceededException();
        }
        this.payload = payload;
        this.size += payload.length;
    }

    @Override
    public void send(ByteBuffer out) {
        super.send(out);
        out.putShort((short) this.sessionID);
        out.put(this.payload);
    }

    public static OnionAuthSessionHS1 parse(ByteBuffer buf)
            throws MessageParserException {
        OnionAuthSessionHS1 message;
        byte[] payload;

        if (buf.remaining() <= 4) {
            throw new MessageParserException("Missing payload");
        }
        long id = protocol.Message.unsignedLongFromInt(buf.getInt());
        payload = new byte[buf.remaining()];
        buf.get(payload);
        try {
            message = new OnionAuthSessionHS1((int) id, 0, payload);
        } catch (MessageSizeExceededException ex) {
            throw new MessageParserException("invalid message encoding");
        }
        return message;
    }
}
