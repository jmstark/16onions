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
import java.security.InvalidKeyException;
import java.security.interfaces.RSAPublicKey;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import protocol.Message;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;
import util.SecurityHelper;

/**
 *
 * @author totakura
 */
@EqualsAndHashCode(callSuper = true, exclude = "pkey")
public class OnionAuthSessionStartMessage extends OnionAuthApiMessage {

    @Getter private final byte[] keyEnc;
    @Getter private final RSAPublicKey pkey;
    @Getter private final long requestID;

    /**
     * Return new OnionAuthSessionStartMessage.
     *
     * @param pkey the public key
     * @throws protocol.MessageSizeExceededException
     */
    public OnionAuthSessionStartMessage(long requestID, RSAPublicKey pkey)
            throws MessageSizeExceededException {
        assert (requestID <= Message.UINT32_MAX);
        this.size += 4; //for reserved 32 bits
        this.requestID = requestID;
        this.size += 4;
        this.pkey = pkey;
        this.keyEnc = SecurityHelper.encodeRSAPublicKey(pkey);
        this.addHeader(Protocol.MessageType.API_AUTH_SESSION_START);
        if ((this.size + this.keyEnc.length) > Protocol.MAX_MESSAGE_SIZE) {
            throw new MessageSizeExceededException();
        }
        this.size += this.keyEnc.length;
    }

    @Override
    public void send(ByteBuffer out) {
        super.send(out);
        super.sendEmptyBytes(out, 4); //reserved bits
        out.putInt((int) requestID);
        out.put(keyEnc);
    }

    public static OnionAuthSessionStartMessage parse(ByteBuffer buf)
            throws MessageParserException {
        byte[] enc;
        OnionAuthSessionStartMessage message;
        RSAPublicKey pkey;
        long requestID;

        buf.position(buf.position() + 4); //skip remaining
        requestID = Message.unsignedLongFromInt(buf.getInt());
        enc = new byte[buf.remaining()];
        buf.get(enc);
        try {
            pkey = SecurityHelper.getRSAPublicKeyFromEncoding(enc);
        } catch (InvalidKeyException ex) {
            throw new MessageParserException("Invalid key");
        }
        try {
            message = new OnionAuthSessionStartMessage(requestID, pkey);
        } catch (MessageSizeExceededException ex) {
            throw new MessageParserException("Size exceeded");
        }
        return message;
    }

}
