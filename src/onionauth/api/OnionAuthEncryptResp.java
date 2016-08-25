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
import java.util.Arrays;
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
public class OnionAuthEncryptResp extends OnionAuthApiMessage {

    private final int id;
    private final byte[] payload;

    public OnionAuthEncryptResp(int id, byte[] payload) throws MessageSizeExceededException {
        this.addHeader(Protocol.MessageType.API_AUTH_LAYER_ENCRYPT_RESP);
        this.id = id;
        this.size += 4; //2 id + 2 reserved
        this.payload = payload;
        this.size += payload.length;
        if (this.size > Protocol.MAX_MESSAGE_SIZE) {
            throw new MessageSizeExceededException();
        }
    }

    /**
     * Get the response ID.
     *
     * The response ID matches a previous request.
     *
     * @return the response ID
     */
    public int getId() {
        return id;
    }

    /**
     * Get the encrypted payload.
     *
     * @return the encrypted payload
     */
    public byte[] getPayload() {
        return payload;
    }

    @Override
    public void send(ByteBuffer out) {
        super.send(out);
        out.putShort((short) this.id);
        this.sendEmptyBytes(out, 2); //reserved
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
        int id;
        byte[] payload;

        if (buf.remaining() <= 4) {
            throw new MessageParserException("Message size is too small");
        }
        id = Message.unsignedIntFromShort(buf.getShort());
        buf.getShort(); //2 reserved
        payload = new byte[buf.remaining()];
        buf.get(payload);
        OnionAuthEncryptResp message;
        try {
            message = new OnionAuthEncryptResp(id, payload);
        } catch (MessageSizeExceededException ex) {
            throw new MessageParserException();
        }
        return message;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + this.id;
        hash = 31 * hash + Arrays.hashCode(this.payload);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final OnionAuthEncryptResp other = (OnionAuthEncryptResp) obj;
        if (this.id != other.id) {
            return false;
        }
        if (!Arrays.equals(this.payload, other.payload)) {
            return false;
        }
        return true;
    }
}
