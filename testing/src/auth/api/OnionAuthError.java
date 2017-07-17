/*
 * Copyright (C) 2017 Sree Harsha Totakura <sreeharsha@totakura.in>
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
package auth.api;

import java.nio.ByteBuffer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import protocol.Message;
import protocol.MessageParserException;
import protocol.Protocol;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
@EqualsAndHashCode(callSuper = true)
public class OnionAuthError extends OnionAuthApiMessage {
    @Getter private long requestID;

    public OnionAuthError(long requestID) {
        assert (requestID <= Message.UINT32_MAX);
        super.addHeader(Protocol.MessageType.API_AUTH_ERROR);
        this.size += 4; // 4 bytes for reserved
        this.requestID = requestID;
        this.size += 4; // 4 bytes for requestID
    }

    @Override
    public void send(ByteBuffer out) {
        super.send(out);
        super.sendEmptyBytes(out, 4);
        out.putInt((int) requestID);
    }

    public static OnionAuthError parse(ByteBuffer buf) throws
            MessageParserException {
        long requestID;

        if (buf.remaining() != 8) {
            throw new MessageParserException("Unknown format");
        }
        buf.getInt(); //read out 4 bytes for reserved
        requestID = Message.unsignedLongFromInt(buf.getInt());
        return new OnionAuthError(requestID);
    }
}
