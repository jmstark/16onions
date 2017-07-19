/*
 * Copyright (C) 2017 Sree Harsha Totakura <sreeharsha@totakura.in>
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
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
@EqualsAndHashCode(callSuper = true)
public class OnionAuthCipherEncrypt extends OnionAuthApiMessage {

    @Getter private boolean isCipher;
    @Getter private long requestID;
    @Getter private int sessionID;
    @Getter private byte[] payload;

    public OnionAuthCipherEncrypt(boolean isCipher,
            long requestID,
            int sessionID, byte[] payload) throws MessageSizeExceededException {
        super.addHeader(Protocol.MessageType.API_AUTH_CIPHER_ENCRYPT);
        assert (sessionID <= Message.UINT16_MAX);
        assert (requestID <= Message.UINT32_MAX);
        this.size += 4; //reserved field + 1 flag
        this.isCipher = isCipher;
        this.requestID = requestID;
        this.size += 4;
        this.sessionID = sessionID;
        this.size += 2;
        this.size += payload.length;
        if (this.size > Protocol.MAX_MESSAGE_SIZE) {
            throw new MessageSizeExceededException();
        }
        this.payload = payload;
    }

    @Override
    public void send(ByteBuffer out) {
        super.send(out);
        super.sendEmptyBytes(out, 3); //3 bytes of reserved bytes
        out.put((byte) (isCipher ? 1 : 0));
        out.putInt((int) requestID);
        out.putShort((short) sessionID);
        out.put(payload);
    }

    public static OnionAuthCipherEncrypt parse(ByteBuffer buf) throws
            MessageParserException {
        boolean isCipher;
        long requestID;
        int sessionID;
        byte[] payload;

        if (buf.remaining() <= 10) {
            throw new MessageParserException(
                    "Insufficient data to parse message");
        }
        buf.position(buf.position() + 3);
        isCipher = (buf.get() & (byte) 1) == 1;
        requestID = Message.unsignedLongFromInt(buf.getInt());
        sessionID = Message.unsignedIntFromShort(buf.getShort());
        payload = new byte[buf.remaining()];
        buf.get(payload);
        OnionAuthCipherEncrypt message = null;
        try {
            message = new OnionAuthCipherEncrypt(isCipher,                            requestID,
                            sessionID,
                            payload);
        } catch (MessageSizeExceededException ex) {
            Logger.getLogger(OnionAuthCipherEncrypt.class.getName()).
                    log(Level.SEVERE,
                            "This should not happen; please report this as bug",
                            ex);
            assert (false);
        }
        return message;
    }
}
