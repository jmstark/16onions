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
import java.util.Arrays;
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
public class OnionAuthEncrypt extends OnionAuthApiMessage {

    @Getter private long requestID;
    @Getter private int[] sessions;
    @Getter private byte[] payload;

    /**
     * Create a message to encrypt given data using layered encryption.
     *
     * @param sessions the sessions of the layer. The session should have
     * previously been created.
     * @param requestID the request ID. This is used to match responses to
     * requests.
     * @param payload the data to be encrypted.
     * @throws MessageSizeExceededException
     */
    public OnionAuthEncrypt(long requestID, int[] sessions, byte[] payload)
            throws MessageSizeExceededException {
        this.addHeader(Protocol.MessageType.API_AUTH_LAYER_ENCRYPT);
        if (sessions.length > 255) {
            throw new MessageSizeExceededException(
                    "Number of sessions cannot be more that 255");
        }
        assert (requestID <= Message.UINT32_MAX);
        for (int session : sessions) {
            assert (session <= Message.UINT16_MAX);
        }
        this.size += 4; //2 reserved + 1 layer count byte + 1 reserved byte
        this.requestID = requestID;
        this.size += 4;
        this.sessions = sessions;
        this.size += sessions.length * 2; // 4 bytes for each session
        this.payload = payload;
        this.size += payload.length;
        if (this.size > Protocol.MAX_MESSAGE_SIZE) {
            throw new MessageSizeExceededException();
        }
    }

    @Override
    public void send(ByteBuffer out) {
        super.send(out);
        super.sendEmptyBytes(out, 2);
        out.put((byte) sessions.length);
        super.sendEmptyBytes(out, 1);
        out.putInt((int) requestID);
        for (int session : sessions) {
            out.putShort((short) session);
        }
        out.put(payload);
    }

    public static OnionAuthEncrypt parse(ByteBuffer buf)
            throws MessageParserException {
        OnionAuthEncrypt message;
        short layerCount;
        long requestID;
        int[] sessions;
        byte[] payload;

        if (buf.remaining() < 11) //1 added header + 1 session + 1 byte payload
        {
            throw new MessageParserException("Message is too small");
        }
        buf.getShort(); //read out reserved portion
        layerCount = Message.unsignedShortFromByte(buf.get());
        buf.get(); //skip reserved
        requestID = Message.unsignedLongFromInt(buf.getInt());
        assert (layerCount <= 255);
        sessions = new int[layerCount];
        for (int index = 0; index < sessions.length; index++) {
            sessions[index] = Message.unsignedIntFromShort(buf.getShort());
        }
        payload = new byte[buf.remaining()];
        buf.get(payload);
        try {
            message = new OnionAuthEncrypt(requestID, sessions, payload);
        } catch (MessageSizeExceededException ex) {
            throw new MessageParserException("Message size exceeded");
        }
        return message;
    }
}
