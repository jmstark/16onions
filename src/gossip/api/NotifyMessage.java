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
package gossip.api;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import protocol.Message;
import protocol.MessageParserException;
import protocol.Protocol;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class NotifyMessage extends ApiMessage {
    protected int datatype;

    public NotifyMessage(int datatype) {
        assert (65535 >= datatype);
        super.addHeader(Protocol.MessageType.API_GOSSIP_NOTIFY);
        this.datatype = datatype;
        this.size += 2 + 2; //2 reserved; 2 datatype
    }

    public int getDatatype() {
        return datatype;
    }

    @Override
    public void send(ByteBuffer out) {
        super.send(out);
        super.sendEmptyBytes(out, 2); //2 bytes reserved
        out.putShort((short) datatype);
    }

    static ApiMessage parse(ByteBuffer buf) throws MessageParserException {
        ApiMessage message;
        int datatype;

        try {
        buf.position(buf.position() + 2);//skip over the reserved part
        datatype = Message.unsignedIntFromShort(buf.getShort());
            message = new NotifyMessage(datatype);
        } catch (BufferUnderflowException | IllegalArgumentException exp) {
            throw new MessageParserException();
        }
        return message;
    }

}
