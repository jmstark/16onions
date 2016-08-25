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
package onionauth.api;

import java.nio.ByteBuffer;
import protocol.MessageParserException;
import protocol.Protocol;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class OnionAuthClose extends OnionAuthApiMessage {

    int sessionID;

    public OnionAuthClose(int sessionID) {
        super.addHeader(Protocol.MessageType.API_AUTH_SESSION_CLOSE);
        assert (sessionID <= ((1 << 16) - 1));
        this.sessionID = sessionID;
        this.size += 4; //2 bytes reserved; 2 for sessionID
    }

    public long getSessionID() {
        return sessionID;
    }

    public void send(ByteBuffer out) {
        super.send(out);
        super.sendEmptyBytes(out, 2); //reserved
        out.putShort((short) sessionID);
    }

    public static OnionAuthClose parse(ByteBuffer buf) throws
            MessageParserException {
        int id;
        int remaining;

        remaining = buf.remaining();

        if ((remaining < 4) || (remaining > 4)) {
            throw new MessageParserException("unable to parse message");
        }
        buf.getShort();//reserved
        id = protocol.Message.unsignedIntFromShort(buf.getShort());
        OnionAuthClose message = new OnionAuthClose(id);
        return message;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + (int) (this.sessionID ^ (this.sessionID >>> 32));
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
        final OnionAuthClose other = (OnionAuthClose) obj;
        if (this.sessionID != other.sessionID) {
            return false;
        }
        return true;
    }
}
