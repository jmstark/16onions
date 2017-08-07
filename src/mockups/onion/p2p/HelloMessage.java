/*
 * Copyright (C) 2016 totakura
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
package mockups.onion.p2p;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.interfaces.RSAPublicKey;
import lombok.EqualsAndHashCode;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;
import util.SecurityHelper;

/**
 *
 * @author totakura
 */
@EqualsAndHashCode(callSuper = true)
public class HelloMessage extends OnionP2PMessage {

    private static HelloMessage hello = new HelloMessage();

    public HelloMessage() {
        this.addHeader(Protocol.MessageType.ONION_HELLO);
    }

    public static HelloMessage parse(ByteBuffer buf) throws
            MessageParserException {

        return hello;
    }

}
