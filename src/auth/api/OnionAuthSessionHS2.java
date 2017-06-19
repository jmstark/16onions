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
import java.util.logging.Level;
import java.util.logging.Logger;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol.MessageType;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class OnionAuthSessionHS2 extends OnionAuthSessionHS1 {

    private static final MessageType TYPE = MessageType.API_AUTH_SESSION_HS2;

    public OnionAuthSessionHS2(int sessionID, long requestID, byte[] payload)
            throws MessageSizeExceededException {
        super(sessionID, requestID, payload);
        this.changeMessageType(TYPE);
    }

    public static OnionAuthSessionHS2 parse(ByteBuffer buf)
            throws MessageParserException {
        OnionAuthSessionHS2 message;
        OnionAuthSessionHS1 parent;
        parent = OnionAuthSessionHS1.parse(buf);
        try {
            message = new OnionAuthSessionHS2(parent.getSessionID(),
                    parent.getRequestID(), parent.getPayload());
        } catch (MessageSizeExceededException ex) {
            throw new MessageParserException();
        }
        return message;
    }
}
