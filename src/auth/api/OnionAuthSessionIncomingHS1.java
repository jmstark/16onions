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

    @Getter private final RSAPublicKey sourceKey;
    @Getter private final byte[] payload;
    private final byte[] keyEnc;
    @Getter private final long requestID;

    public OnionAuthSessionIncomingHS1(long requestID,
            RSAPublicKey key, byte[] payload) throws
            MessageSizeExceededException {
        this.addHeader(Protocol.MessageType.API_AUTH_SESSION_INCOMING_HS1);
        this.size += 4; //reserved
        assert (requestID <= Message.UINT32_MAX);
        this.requestID = requestID;
        this.size += 4;

        this.size += 2; //hostkey size
        this.sourceKey = key;
        this.keyEnc = util.SecurityHelper.encodeRSAPublicKey(key);
        this.size += keyEnc.length;

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
        out.putShort((short) keyEnc.length);
        out.put(keyEnc);
        out.put(payload);
    }

    public static OnionAuthSessionIncomingHS1 parse(ByteBuffer buf)
            throws MessageParserException {
        RSAPublicKey key;
        long requestID;
        byte[] enc;
        byte[] payload;
        int size;

        size = buf.remaining();
        if (size <= 10) {
            throw new MessageParserException(
                    "Message format not recognized");
        }
        buf.getInt();//read out reserved 4  bytes
        size -= 4;
        requestID = Message.unsignedLongFromInt(buf.getInt());
        size -= 4;
        enc = new byte[protocol.Message.unsignedIntFromShort(buf.getShort())];
        size -= 2;
        buf.get(enc);
        size -= enc.length;
        try {
            key = util.SecurityHelper.getRSAPublicKeyFromEncoding(enc);
        } catch (InvalidKeyException ex) {
            throw new MessageParserException("Invalid hostkey in message");
        }
        if (size <= 0) {
            throw new MessageParserException("Message does not contain payload");
        }
        payload = new byte[size];
        buf.get(payload);

        OnionAuthSessionIncomingHS1 message;
        try {
            message = new OnionAuthSessionIncomingHS1(requestID, key, payload);
        } catch (MessageSizeExceededException ex) {
            throw new RuntimeException(
                    "Message size exceeded. The protocol.parser should have caught this");
        }
        return message;
    }
}
