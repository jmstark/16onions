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
package protocol;

import java.nio.ByteBuffer;
import protocol.Protocol.MessageType;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public abstract class MessageHandler<C> {

    private final C closure;

    protected MessageHandler(C closure) {
        this.closure = closure;
    }

    public void parseMessage(ByteBuffer buf) throws MessageParserException {
        int size;
        MessageType type;

        size = buf.getShort();
        type = MessageType.asMessageType(buf.getShort());
        buf.limit(size);
        this.parseMessage(buf, type, this.closure);
    }
    /**
     * Construct message object from the representation in buffer
     *
     * @param buf
     * @return the constructed message object
     * @throws MessageParserException
     */
    public abstract void parseMessage(ByteBuffer buf, MessageType type, C closure)
            throws MessageParserException;
}
