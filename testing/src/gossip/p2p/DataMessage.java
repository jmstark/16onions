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

    static DataMessage parse(ByteBuffer buf) throws MessageParserException {
        DataMessage message;
        int data_type;
        int data_size;
        byte[] data;

        data_size = buf.remaining() - 2; // 2 bytes for datatype
        if (data_size < 0) {
            throw new MessageParserException();
        }
        data = new byte[data_size];
        data_type = Message.getUnsignedShort(buf);
        buf.get(data);
        try {
            message = DataMessage.create(data_type, data);
        } catch (MessageSizeExceededException ex) {
            throw new RuntimeException("This should not happen; please report this.");
        }
        return message;
    }

    private final Page page;

    DataMessage(Page page) throws MessageSizeExceededException {
        super();
        byte[] data = page.getData();
        this.page = page;
        int datatype = page.getType();
        this.addHeader(Protocol.MessageType.GOSSIP_DATA);
        this.size += 2; //data_type as short
        this.size += data.length;
        if (this.size > Protocol.MAX_MESSAGE_SIZE) {
            throw new MessageSizeExceededException();
        }
    }

    public Page getPage() {
        return page;
    }

    @Override
    public void send(ByteBuffer out) {
        super.send(out);
        out.putShort((short) this.page.getType());
        out.put(this.page.getData());
    }

    static DataMessage create(int datatype, byte[] data) throws
            MessageSizeExceededException {
        return new DataMessage(new Page(datatype, data));
    }
}
