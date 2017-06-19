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
import protocol.Message;
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
        assert (sessionID <= Message.UINT16_MAX);
        assert (requestID <= Message.UINT32_MAX);
        this.addHeader(Protocol.MessageType.API_AUTH_SESSION_HS1);
        this.size += 2;// for reserved
        this.sessionID = sessionID;
        this.size += 2;
        this.requestID = requestID;
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
        super.sendEmptyBytes(out, 2);//reserved
        out.putShort((short) this.sessionID);
        out.putInt((int) requestID);
        out.put(this.payload);
    }

    public static OnionAuthSessionHS1 parse(ByteBuffer buf)
            throws MessageParserException {
        OnionAuthSessionHS1 message;
        byte[] payload;
        long requestID;
        int sessionID;

        if (buf.remaining() <= 4) {
            throw new MessageParserException("Missing payload");
        }
        buf.getShort(); //read out reserved part
        sessionID = Message.unsignedIntFromShort(buf.getShort());
        requestID = Message.unsignedLongFromInt(buf.getInt());
        payload = new byte[buf.remaining()];
        buf.get(payload);
        try {
            message = new OnionAuthSessionHS1(sessionID, requestID, payload);
        } catch (MessageSizeExceededException ex) {
            throw new MessageParserException("Invalid message encoding");
        }
        return message;
    }
}
