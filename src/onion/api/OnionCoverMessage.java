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
public class OnionCoverMessage extends OnionApiMessage {

    private final int coverSize;

    public OnionCoverMessage(int coverSize) {
        this.addHeader(Protocol.MessageType.API_ONION_COVER);
        this.coverSize = coverSize;
        size += 4; // cover size and reserved
    }

    public int getCoverSize() {
        return coverSize;
    }

    @Override
    public void send(ByteBuffer out) {
        super.send(out);
        out.putShort((short) coverSize);
        super.sendEmptyBytes(out, 2); //reserved
    }

    public static OnionCoverMessage parse(ByteBuffer buffer) throws
            MessageParserException {
        int cSize;
        cSize = Message.unsignedIntFromShort(buffer.getShort());
        buffer.getShort();//reserved
        return new OnionCoverMessage(cSize);
    }
}
