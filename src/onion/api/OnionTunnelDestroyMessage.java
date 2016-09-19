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
import protocol.Protocol;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class OnionTunnelDestroyMessage extends OnionApiMessage {

    private final long id;

    public OnionTunnelDestroyMessage(long id) {
        assert (id <= UINT32_MAX);
        this.addHeader(Protocol.MessageType.API_ONION_TUNNEL_DESTROY);
        this.id = id;
        this.size += 4;
    }

    public long getId() {
        return id;
    }

    @Override
    public void send(ByteBuffer out) {
        super.send(out);
        out.putInt((int) id);
    }

    public static OnionTunnelDestroyMessage parser(ByteBuffer buffer) throws
            MessageParserException {
        long id;
        id = Message.unsignedLongFromInt(buffer.getInt());
        if (0 != buffer.remaining()) {
            throw new MessageParserException();
        }
        return new OnionTunnelDestroyMessage(id);
    }
}
