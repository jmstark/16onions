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
package gossip.p2p;

import java.nio.ByteBuffer;
import protocol.Message;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;

/**
 * Message which contains data that is being announced through Gossip
 *
 * @author totakura
 */
class DataMessage extends PeerMessage {

    static PeerMessage parse(ByteBuffer buf) throws MessageParserException {
        PeerMessage message;
        int data_type;
        int data_size;
        byte[] data;

        data_size = buf.remaining() - 2;
        if (data_size < 0) {
            throw new MessageParserException();
        }
        data = new byte[data_size];
        data_type = Message.getUnsignedShort(buf);
        buf.get(data);
        try {
            message = new DataMessage(data_type, data);
        } catch (MessageSizeExceededException ex) {
            throw new RuntimeException("This should not happen; please report this.");
        }
        return message;
    }

    final private byte[] data;
    final private int data_type;

    DataMessage(int data_type, byte[] data) throws MessageSizeExceededException {
        super();
        this.addHeader(Protocol.MessageType.GOSSIP_DATA);
        this.size += 2; //data_type as short
        this.size += data.length;
        if (this.size > Protocol.MAX_MESSAGE_SIZE) {
            throw new MessageSizeExceededException();
        }
        this.data = data;
        this.data_type = data_type;
    }

    public byte[] getData() {
        return data;
    }

    public int getData_type() {
        return data_type;
    }

    @Override
    public void send(ByteBuffer out) {
        super.send(out);
        out.putShort((short) this.data_type);
        out.put(out);
    }
}
