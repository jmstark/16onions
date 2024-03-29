package auth.api;

import java.nio.ByteBuffer;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;

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

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class OnionAuthDecrypt extends OnionAuthEncrypt {

    public OnionAuthDecrypt(long requestID,
            int[] sessions, byte[] payload) throws
            MessageSizeExceededException {
        super(requestID, sessions, payload);
        this.changeMessageType(Protocol.MessageType.API_AUTH_LAYER_DECRYPT);
    }

    public static OnionAuthDecrypt parse(ByteBuffer buf) throws
            MessageParserException {
        OnionAuthEncrypt parent;
        parent = OnionAuthEncrypt.parse(buf);
        OnionAuthDecrypt message;
        try {
            message = new OnionAuthDecrypt(parent.getRequestID(),
                    parent.getSessions(),
                    parent.getPayload());
        } catch (MessageSizeExceededException ex) {
            throw new MessageParserException();
        }
        return message;
    }
}
