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
package auth.api;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Objects;
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
public class OnionAuthSessionIncomingHS1 extends OnionAuthApiMessage {

    @Getter private final byte[] payload;
    @Getter private final long requestID;

    public OnionAuthSessionIncomingHS1(long requestID, byte[] payload) throws
            MessageSizeExceededException {
        this.addHeader(Protocol.MessageType.API_AUTH_SESSION_INCOMING_HS1);
        this.size += 4; //reserved
        assert (requestID <= Message.UINT32_MAX);
        this.requestID = requestID;
        this.size += 4;
        this.payload = payload;
        this.size += payload.length;

        if (this.size > Protocol.MAX_MESSAGE_SIZE) {
            throw new MessageSizeExceededException();
        }
    }

    @Override
    public void send(ByteBuffer out) {
        super.send(out);
        this.sendEmptyBytes(out, 4); //reserved 4 bytes
        out.putInt((int) requestID);
        out.put(payload);
    }

    public static OnionAuthSessionIncomingHS1 parse(ByteBuffer buf)
            throws MessageParserException {
        long requestID;
        byte[] payload;

        if (buf.remaining() <= 12) {
            throw new MessageParserException(
                    "Message format not recognized");
        }
        buf.getInt();//read out reserved 4  bytes
        requestID = Message.unsignedLongFromInt(buf.getInt());
        payload = new byte[buf.remaining()];
        buf.get(payload);

        OnionAuthSessionIncomingHS1 message;
        try {
            message = new OnionAuthSessionIncomingHS1(requestID, payload);
        } catch (MessageSizeExceededException ex) {
            throw new RuntimeException(
                    "Message size exceeded. The protocol.parser should have caught this");
        }
        return message;
    }
}
