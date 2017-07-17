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
package onion.api;

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
public class OnionTunnelDataMessage extends OnionApiMessage {

    private final long id;
    private final byte[] data;

    public OnionTunnelDataMessage(long id, byte[] data) throws
            MessageSizeExceededException {
        this.addHeader(Protocol.MessageType.API_ONION_TUNNEL_DATA);
        this.id = id;
        size += 4; //for id
        this.data = data;
        size += data.length;
        if (size > Protocol.MAX_MESSAGE_SIZE) {
            throw new MessageSizeExceededException();
        }
    }

    public long getId() {
        return id;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public void send(ByteBuffer out) {
        super.send(out);
        out.putInt((int) id);
        out.put(data);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + (int) (this.id ^ (this.id >>> 32));
        hash = 89 * hash + Arrays.hashCode(this.data);
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
        final OnionTunnelDataMessage other = (OnionTunnelDataMessage) obj;
        if (this.id != other.id) {
            return false;
        }
        if (!Arrays.equals(this.data, other.data)) {
            return false;
        }
        return true;
    }

    public static OnionTunnelDataMessage parse(ByteBuffer buffer) throws
            MessageParserException {
        long id;
        byte[] data;
        OnionTunnelDataMessage message;

        id = Message.unsignedLongFromInt(buffer.getInt());
        data = new byte[buffer.remaining()];
        buffer.get(data);
        try {
            message = new OnionTunnelDataMessage(id, data);
        } catch (MessageSizeExceededException ex) {
            throw new RuntimeException("This is a bug; please report");
        }
        return message;
    }

}
