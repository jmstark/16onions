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
package mockups.onion.p2p;

import java.nio.ByteBuffer;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
class DataMessage extends OnionP2PMessage {

    private byte[] data;

    DataMessage(byte[] data) throws MessageSizeExceededException {
        this.addHeader(Protocol.MessageType.ONION_DATA);
        this.size += data.length;
        if (this.size > Protocol.MAX_MESSAGE_SIZE) {
            throw new MessageSizeExceededException();
        }
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public void send(ByteBuffer out) {
        super.send(out);
        out.put(data);
    }

    public static DataMessage parse(ByteBuffer buffer) throws
            MessageParserException {
        byte[] data;
        data = new byte[buffer.remaining()];
        buffer.get(data);
        try {
            return new DataMessage(data);
        } catch (MessageSizeExceededException ex) {
            throw new RuntimeException("This is a bug; please report.");
        }
    }

}
