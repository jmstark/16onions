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
public class OnionTunnelIncomingMessage extends OnionApiMessage {

    private final long tunnelID;
    private final byte[] keyEncoding;

    public OnionTunnelIncomingMessage(long tunnelID, byte[] keyEncoding) throws
            MessageSizeExceededException {
        this.addHeader(Protocol.MessageType.API_ONION_TUNNEL_INCOMING);
        this.tunnelID = tunnelID;
        this.size += 4; // tunnel ID
        this.keyEncoding = keyEncoding;
        this.size += keyEncoding.length;
        if (this.size > Protocol.MAX_MESSAGE_SIZE) {
            throw new MessageSizeExceededException();
        }
    }

    public long getTunnelID() {
        return tunnelID;
    }

    public byte[] getKeyEncoding() {
        return keyEncoding;
    }

    @Override
    public void send(ByteBuffer out) {
        super.send(out);
        out.putInt((int) tunnelID);
        out.put(keyEncoding);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + (int) (this.tunnelID ^ (this.tunnelID >>> 32));
        hash = 41 * hash + Arrays.hashCode(this.keyEncoding);
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
        final OnionTunnelIncomingMessage other = (OnionTunnelIncomingMessage) obj;
        if (this.tunnelID != other.tunnelID) {
            return false;
        }
        if (!Arrays.equals(this.keyEncoding, other.keyEncoding)) {
            return false;
        }
        return true;
    }

    public static OnionTunnelIncomingMessage parser(ByteBuffer buffer) throws
            MessageParserException {
        long id;
        id = Message.unsignedLongFromInt(buffer.getInt());
        byte[] encoding;
        encoding = new byte[buffer.remaining()];
        buffer.get(encoding);
        try {
            return new OnionTunnelIncomingMessage(id, encoding);
        } catch (MessageSizeExceededException ex) {
            throw new RuntimeException("This is a bug; please report");
        }
    }
}
