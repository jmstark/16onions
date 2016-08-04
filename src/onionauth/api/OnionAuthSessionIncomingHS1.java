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
package onionauth.api;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.interfaces.RSAPublicKey;
import java.util.logging.Level;
import java.util.logging.Logger;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class OnionAuthSessionIncomingHS1 extends OnionAuthApiMessage {

    private final RSAPublicKey sourceKey;
    private final byte[] payload;
    private final byte[] keyEnc;

    public OnionAuthSessionIncomingHS1(RSAPublicKey key, byte[] payload) throws
            MessageSizeExceededException {
        this.addHeader(Protocol.MessageType.API_AUTH_SESSION_INCOMING_HS1);
        this.size += 2; //reserved
        this.size += 2; //hostkey size

        this.sourceKey = key;
        this.keyEnc = tools.SecurityHelper.encodeRSAPublicKey(key);
        this.size += keyEnc.length;

        this.payload = payload;
        this.size += payload.length;

        if (this.size > Protocol.MAX_MESSAGE_SIZE) {
            throw new MessageSizeExceededException();
        }
    }

    public RSAPublicKey getSourceKey() {
        return sourceKey;
    }

    public byte[] getPayload() {
        return payload;
    }

    @Override
    public void send(ByteBuffer out) {
        super.send(out);
        this.sendEmptyBytes(out, 2); //reserved 2 bytes
        out.putShort((short) keyEnc.length);
        out.put(keyEnc);
        out.put(payload);
    }

    public static OnionAuthSessionIncomingHS1 parse(ByteBuffer buf)
            throws MessageParserException {
        RSAPublicKey key;
        byte[] enc;
        byte[] payload;
        int size;

        size = buf.remaining();
        if (size <= 4) {
            throw new MessageParserException(
                    "Message does not contain hostkey/payload");
        }
        buf.getShort();//reserved
        size -= 2;
        enc = new byte[protocol.Message.unsignedIntFromShort(buf.getShort())];
        size -= 2;
        buf.get(enc);
        size -= enc.length;
        try {
            key = tools.SecurityHelper.getRSAPublicKeyFromEncoding(enc);
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
            message = new OnionAuthSessionIncomingHS1(key, payload);
        } catch (MessageSizeExceededException ex) {
            throw new RuntimeException(
                    "Message size exceeded. The protocol.parser should have caught this");
        }
        return message;
    }
}
