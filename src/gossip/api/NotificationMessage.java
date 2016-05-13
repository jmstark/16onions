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
package gossip.api;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import protocol.Message;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;

/**
 *
 * @author totakura
 */
public class NotificationMessage extends NotifyMessage {

    byte[] data;

    public NotificationMessage(int datatype, byte[] data)
            throws MessageSizeExceededException {
        super(datatype);
        this.changeMessageType(Protocol.MessageType.API_GOSSIP_NOTIFICATION);
        this.data = data;
        if (Protocol.MAX_MESSAGE_SIZE < (this.size + data.length)) {
            throw new MessageSizeExceededException();
        }
        this.size += data.length;
    }

    @Override
    public void send(ByteBuffer out) {
        super.send(out);
        out.put(data);
    }

    /**
     * Parse a tokenized buffer to construct this message
     *
     * @param buf
     * @return
     */
    static ApiMessage parse(ByteBuffer buf) throws MessageParserException {
        byte[] data;
        int datatype;
        ApiMessage message;

        try {
        buf.position(buf.position() + 2); //skip over the reserved part
        datatype = Message.unsignedIntFromShort(buf.getShort());
        data = new byte[buf.remaining()];
            buf.get(data);
            message = new NotificationMessage(datatype, data);
        } catch (BufferUnderflowException | IllegalArgumentException |
                MessageSizeExceededException exp) {
            throw new MessageParserException();
        }
        return message;
    }
}
