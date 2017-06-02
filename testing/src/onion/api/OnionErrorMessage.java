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
import java.util.Objects;
import protocol.Message;
import protocol.MessageParserException;
import protocol.Protocol;
import protocol.UnknownMessageTypeException;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class OnionErrorMessage extends OnionApiMessage {

    private final Protocol.MessageType requestType;
    private final long id;

    public OnionErrorMessage(Protocol.MessageType type, long id) {
        switch (type) {
            case API_ONION_TUNNEL_BUILD:
            case API_ONION_TUNNEL_READY:
            case API_ONION_TUNNEL_INCOMING:
            case API_ONION_TUNNEL_DESTROY:
            case API_ONION_TUNNEL_DATA:
            case API_ONION_COVER:
                break;
            default:
                assert (false);
        }
        this.addHeader(Protocol.MessageType.API_ONION_ERROR);
        this.size += 4; //request type and reserved
        this.requestType = type;
        this.size += 4; //tunnel id
        this.id = id;
    }

    public Protocol.MessageType getRequestType() {
        return requestType;
    }

    public long getId() {
        return id;
    }

    @Override
    public void send(ByteBuffer out) {
        super.send(out);
        out.putShort((short) requestType.getNumVal());
        super.sendEmptyBytes(out, 2);
        out.putInt((int) id);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 37 * hash + Objects.hashCode(this.requestType);
        hash = 37 * hash + (int) (this.id ^ (this.id >>> 32));
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
        final OnionErrorMessage other = (OnionErrorMessage) obj;
        if (this.id != other.id) {
            return false;
        }
        if (this.requestType != other.requestType) {
            return false;
        }
        return true;
    }

    public static OnionErrorMessage parser(ByteBuffer buffer) throws
            MessageParserException {
        long id;
        Protocol.MessageType rType;

        try {
            rType = Protocol.MessageType.asMessageType(buffer.getShort());
        } catch (UnknownMessageTypeException ex) {
            throw new MessageParserException(
                    "Unknown request type for ONION ERROR");
        }
        buffer.getShort(); //skip reserved
        id = Message.unsignedLongFromInt(buffer.getInt());
        return new OnionErrorMessage(rType, id);
    }
}
