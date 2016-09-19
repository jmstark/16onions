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
import protocol.Message;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class OnionTunnelData extends OnionApiMessage {

    private final long id;
    private final byte[] data;

    public OnionTunnelData(long id, byte[] data) throws
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

    public static OnionTunnelData parse(ByteBuffer buffer) throws
            MessageParserException {
        long id;
        byte[] data;
        OnionTunnelData message;

        id = Message.unsignedLongFromInt(buffer.getInt());
        data = new byte[buffer.remaining()];
        buffer.get(data);
        try {
            message = new OnionTunnelData(id, data);
        } catch (MessageSizeExceededException ex) {
            throw new RuntimeException("This is a bug; please report");
        }
        return message;
    }

}
