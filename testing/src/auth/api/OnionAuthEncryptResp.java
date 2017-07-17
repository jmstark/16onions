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
import java.util.Arrays;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import protocol.Message;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;

/**
 * The onion auth encrypt response message.
 *
 * This message is given as a response for OnionAuthEncrypt message
 *
 * @author totakura
 */
@EqualsAndHashCode(callSuper = true)
public class OnionAuthEncryptResp extends OnionAuthApiMessage {

    @Getter private final long requestID;
    @Getter private final byte[] payload;

    public OnionAuthEncryptResp(long requestID, byte[] payload)
            throws MessageSizeExceededException {
        this.addHeader(Protocol.MessageType.API_AUTH_LAYER_ENCRYPT_RESP);
        this.size += 4; //reserved
        this.requestID = requestID;
        this.size += 4; //4 bytes for requestID
        this.payload = payload;
        this.size += payload.length;
        if (this.size > Protocol.MAX_MESSAGE_SIZE) {
            throw new MessageSizeExceededException();
        }
    }

    @Override
    public void send(ByteBuffer out) {
        super.send(out);
        super.sendEmptyBytes(out, 4); //reserved
        out.putInt((int) this.requestID);
        out.put(payload);
    }

    /**
     * Create a OnionAuthEncryptedResp message by parsing the given buffer
     *
     * @param buf the buffer to parse the message data from
     * @return the message
     * @throws MessageParserException upon parsing exception
     */
    public static OnionAuthEncryptResp parse(ByteBuffer buf)
            throws MessageParserException {
        long requestID;
        byte[] payload;

        if (buf.remaining() <= 4) {
            throw new MessageParserException("Message size is too small");
        }
        buf.getInt();
        requestID = Message.unsignedLongFromInt(buf.getInt());
        payload = new byte[buf.remaining()];
        buf.get(payload);
        OnionAuthEncryptResp message;
        try {
            message = new OnionAuthEncryptResp(requestID, payload);
        } catch (MessageSizeExceededException ex) {
            throw new MessageParserException();
        }
        return message;
    }
}
