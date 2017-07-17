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
package com.voidphone.api.auth;

import java.nio.ByteBuffer;
import java.util.Arrays;
import protocol.Message;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class OnionAuthEncrypt extends OnionAuthApiMessage {

    private int id;
    private long[] sessions;
    private byte[] payload;

    /**
     * Create a message to encrypt given data using layered encryption.
     *
     * @param sessions the sessions of the layer. The session should have
     * previously been created.
     * @param id the request ID. This is used to match responses to requests.
     * @param payload the data to be encrypted.
     * @throws MessageSizeExceededException
     */
    public OnionAuthEncrypt(int id, long[] sessions, byte[] payload)
            throws MessageSizeExceededException {
        this.addHeader(Protocol.MessageType.API_AUTH_LAYER_ENCRYPT);
        if (sessions.length > 255) {
            throw new MessageSizeExceededException(
                    "Number of sessions cannot be more that 255");
        }
        assert (id <= ((1 << 16) - 1));
        this.size += 2; //1 layer count byte + 1 reserved byte
        this.id = id;
        this.size += 2;
        this.sessions = sessions;
        this.size += sessions.length * 4; // 4 bytes for each session
        this.payload = payload;
        this.size += payload.length;
        if (this.size > Protocol.MAX_MESSAGE_SIZE) {
            throw new MessageSizeExceededException();
        }
    }

    /**
     * Get the request ID of this message
     *
     * @return the request ID
     */
    public int getId() {
        return id;
    }

    /**
     * Get the session IDs for layered encryption
     *
     * @return the session IDs
     */
    public long[] getSessions() {
        return sessions;
    }

    /**
     * Get the payload of the message.
     *
     * The payload will be encrypted by the Onion Auth service.
     *
     * @return the payload
     */
    public byte[] getPayload() {
        return payload;
    }

    @Override
    public void send(ByteBuffer out) {
        super.send(out);
        out.put((byte) sessions.length);
        super.sendEmptyBytes(out, 1);
        out.putShort((short) id);
        for (long session : sessions) {
            out.putInt((int) session);
        }
        out.put(payload);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 17 * hash + this.id;
        hash = 17 * hash + Arrays.hashCode(this.sessions);
        hash = 17 * hash + Arrays.hashCode(this.payload);
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
        final OnionAuthEncrypt other = (OnionAuthEncrypt) obj;
        if (this.id != other.id) {
            return false;
        }
        if (!Arrays.equals(this.sessions, other.sessions)) {
            return false;
        }
        if (!Arrays.equals(this.payload, other.payload)) {
            return false;
        }
        return true;
    }

    public static OnionAuthEncrypt parse(ByteBuffer buf)
            throws MessageParserException {
        OnionAuthEncrypt message;
        short layerCount;
        int id;
        long[] sessions;
        byte[] payload;

        if (buf.remaining() < 9) //1 added header + 1 session + 1 byte payload
        {
            throw new MessageParserException("Message is too small");
        }
        layerCount = Message.unsignedShortFromByte(buf.get());
        buf.get(); //skip reserved
        id = Message.unsignedIntFromShort(buf.getShort());
        if (layerCount > 255) {
            throw new MessageParserException("Layers cannot be more than 255");
        }
        sessions = new long[layerCount];
        for (int index = 0; index < sessions.length; index++) {
            sessions[index] = Message.unsignedLongFromInt(buf.getInt());
        }
        payload = new byte[buf.remaining()];
        buf.get(payload);
        try {
            message = new OnionAuthEncrypt(id, sessions, payload);
        } catch (MessageSizeExceededException ex) {
            throw new MessageParserException("Message size exceeded");
        }
        return message;
    }
}
