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

import java.nio.ByteBuffer;
import protocol.Message;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;

/**
 *
 * @author totakura
 */
public class AnnounceMessage extends ApiMessage {

    final private short ttl;
    final private int datatype;
    final private byte[] data;

    public AnnounceMessage(short ttl, int datatype, byte[] data)
            throws MessageSizeExceededException {
        assert (ttl <= 255);
        assert (datatype <= 65535);
        this.addHeader(Protocol.MessageType.API_GOSSIP_ANNOUNCE);
        this.ttl = ttl;
        this.size += 1;
        this.size += 1;//reserved byte field after ttl
        this.datatype = datatype;
        this.size += 2;
        this.size += data.length;
        if (this.size > Protocol.MAX_MESSAGE_SIZE) {
            throw new MessageSizeExceededException();
        }
        this.data = data;
    }

    public short getTtl() {
        return ttl;
    }

    public int getDatatype() {
        return datatype;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public void send(ByteBuffer out) {
        super.send(out);
        out.put((byte) ttl);
        super.sendEmptyBytes(out, 1); //reserved
        out.putShort((short) datatype);
        out.put(data);
    }

    public static AnnounceMessage parse(ByteBuffer buf) throws MessageParserException {
        short ttl;
        int datatype;
        byte[] data;
        AnnounceMessage message;

        ttl = Message.unsignedShortFromByte(buf.get());
        buf.position(buf.position() + 1); // reserved byte field
        datatype = Message.unsignedIntFromShort(buf.getShort());
        data = new byte[buf.remaining()];
        buf.get(data);
        try {
            message = new AnnounceMessage(ttl, datatype, data);
        } catch (MessageSizeExceededException ex) {
            throw new RuntimeException(
                    "We parsed well, but failed to deserialize."
                    + "  This is a bug; please report it");
        }
        return message;
    }
}
