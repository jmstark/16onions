/*
 * Copyright (C) 2017 totakura
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
import java.util.logging.Level;
import java.util.logging.Logger;
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
public class OnionAuthCipherDecryptResp extends OnionAuthApiMessage {

    /**
     * Is the decrypted message still a cipher or plaintext
     */
    @Getter private final boolean isCipher;
    @Getter private final long requestID;
    @Getter private final byte[] payload;

    public OnionAuthCipherDecryptResp(boolean isCipher,
            long requestID,
            byte[] payload) throws MessageSizeExceededException {
        super.addHeader(Protocol.MessageType.API_AUTH_CIPHER_DECRYPT_RESP);
        assert (requestID <= Message.UINT32_MAX);
        size += 4; //reserved + cipher flag
        this.isCipher = isCipher;
        size += 4;
        this.requestID = requestID;
        size += payload.length;
        if (Protocol.MAX_MESSAGE_SIZE < size) {
            throw new MessageSizeExceededException();
        }
        this.payload = payload;
    }

    public void send(ByteBuffer out) {
        super.send(out);
        super.sendEmptyBytes(out, 3);
        out.put((byte) (isCipher ? 1 : 0));
        out.putInt((int) requestID);
        out.put(payload);
    }

    public static OnionAuthCipherDecryptResp parse(ByteBuffer buf)
            throws MessageParserException {
        boolean isCipher;
        long requestID;
        byte[] payload;

        if (buf.remaining() < 8) {
            throw new MessageParserException("Message size too small to parse");
        }
        buf.position(buf.position() + 3);
        isCipher = (buf.get() & (byte) 1) == 1;
        requestID = Message.unsignedLongFromInt(buf.getInt());
        payload = new byte[buf.remaining()];
        buf.get(payload);

        OnionAuthCipherDecryptResp message = null;
        try {
            message = new OnionAuthCipherDecryptResp(isCipher, requestID, payload);
        } catch (MessageSizeExceededException ex) {
            Logger.getLogger(OnionAuthCipherDecryptResp.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        }
        return message;
    }
}
