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
package onionauth.api;

import java.nio.ByteBuffer;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;

/**
 *
 * @author totakura
 */
public class OnionAuthSessionHS1 extends OnionAuthApiMessage {

    private long id;
    private byte[] payload;

    public OnionAuthSessionHS1(long id, byte[] payload) throws MessageSizeExceededException {
        assert (id <= ((2 ^ 32) - 1));
        this.addHeader(Protocol.MessageType.API_AUTH_SESSION_HS1);
        this.id = id;
        this.size += 4;
        if ((this.size + payload.length) > Protocol.MAX_MESSAGE_SIZE) {
            throw new MessageSizeExceededException();
        }
        this.payload = payload;
        this.size += payload.length;
    }

    public long getId() {
        return id;
    }

    public byte[] getPayload() {
        return payload;
    }

    @Override
    public void send(ByteBuffer out) {
        out.putInt((int) this.id);
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
            message = new OnionAuthSessionHS1(id, payload);
        } catch (MessageSizeExceededException ex) {
            throw new MessageParserException("invalid message encoding");
        }
        return message;
    }
}
