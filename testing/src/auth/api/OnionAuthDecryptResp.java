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
import protocol.Protocol;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class OnionAuthDecryptResp extends OnionAuthEncryptResp {

    public OnionAuthDecryptResp(long requestID, byte[] payload) throws
            MessageSizeExceededException {
        super(requestID, payload);
        this.changeMessageType(Protocol.MessageType.API_AUTH_LAYER_DECRYPT_RESP);
    }

    public static OnionAuthDecryptResp parse(ByteBuffer buf) throws
            MessageParserException {
        OnionAuthEncryptResp parent;
        parent = OnionAuthEncryptResp.parse(buf);
        OnionAuthDecryptResp message;
        try {
            message = new OnionAuthDecryptResp(parent.getRequestID(), parent.
                    getPayload());
        } catch (MessageSizeExceededException ex) {
            throw new MessageParserException();
        }
        return message;
    }
}
