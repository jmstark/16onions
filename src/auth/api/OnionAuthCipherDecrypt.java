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

/**
 *
 * @author totakura
 */
package auth.api;

import java.nio.ByteBuffer;
import lombok.EqualsAndHashCode;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;

@EqualsAndHashCode(callSuper = true)
public class OnionAuthCipherDecrypt extends OnionAuthCipherEncrypt {

    public OnionAuthCipherDecrypt(long requestID, int sessionID, byte[] payload)
            throws MessageSizeExceededException {
        super(true, requestID, sessionID, payload);
        super.changeMessageType(Protocol.MessageType.API_AUTH_CIPHER_DECRYPT);
    }

    public static OnionAuthCipherDecrypt parse(ByteBuffer buf)
            throws MessageParserException {
        OnionAuthCipherDecrypt message;

        message = (OnionAuthCipherDecrypt) OnionAuthCipherEncrypt.parse(buf);
        message.changeMessageType(Protocol.MessageType.API_AUTH_CIPHER_DECRYPT);
        return message;
    }
}
