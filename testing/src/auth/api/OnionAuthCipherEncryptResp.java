/*
 * Copyright (C) 2017 totakura
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
import lombok.EqualsAndHashCode;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;

/**
 *
 * @author totakura
 */
@EqualsAndHashCode(callSuper = true)
public class OnionAuthCipherEncryptResp extends OnionAuthEncryptResp {

    public OnionAuthCipherEncryptResp(long requestID, byte[] payload) throws MessageSizeExceededException {
        super(requestID, payload);
        super.changeMessageType(Protocol.MessageType.API_AUTH_CIPHER_ENCRYPT_RESP);
    }

    public static OnionAuthCipherEncryptResp parse(ByteBuffer buf)
            throws MessageParserException {
        OnionAuthCipherEncryptResp message = null;

        OnionAuthEncryptResp baseMessage = OnionAuthEncryptResp.parse(buf);
        try {
            message = new OnionAuthCipherEncryptResp(baseMessage.getRequestID(), baseMessage.getPayload());
        } catch (MessageSizeExceededException ex) {
            Logger.getLogger(OnionAuthCipherEncryptResp.class.getName()).log(Level.SEVERE,
                    "This should not happen; please report this as a bug.", ex);
            assert (false);
        }
        return message;
    }
}
