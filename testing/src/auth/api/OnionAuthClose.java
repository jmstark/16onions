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
public class OnionAuthClose extends OnionAuthApiMessage {

    @Getter private int sessionID;

    public OnionAuthClose(int sessionID) {
        super.addHeader(Protocol.MessageType.API_AUTH_SESSION_CLOSE);
        assert (sessionID <= Message.UINT16_MAX);
        this.sessionID = sessionID;
        this.size += 4; //2 bytes reserved; 2 for sessionID
    }

    public void send(ByteBuffer out) {
        super.send(out);
        super.sendEmptyBytes(out, 2); //reserved
        out.putShort((short) sessionID);
    }

    public static OnionAuthClose parse(ByteBuffer buf) throws
            MessageParserException {
        int sessionID;
        int remaining;

        remaining = buf.remaining();

        if ((remaining < 4) || (remaining > 4)) {
            throw new MessageParserException("unable to parse message");
        }
        buf.getShort();//reserved
        sessionID = protocol.Message.unsignedIntFromShort(buf.getShort());
        OnionAuthClose message = new OnionAuthClose(sessionID);
        return message;
    }
}
