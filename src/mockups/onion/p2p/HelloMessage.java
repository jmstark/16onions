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
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;
import util.SecurityHelper;

/**
 *
 * @author totakura
 */
public class HelloMessage extends OnionP2PMessage {

    /**
     * The hostkey of the sender of this message
     */
    private final RSAPublicKey hostkey;

    public HelloMessage(RSAPublicKey hostkey) throws MessageSizeExceededException {
        this.addHeader(Protocol.MessageType.ONION_HELLO);
        this.hostkey = hostkey;
        this.size += SecurityHelper.encodeRSAPublicKey(hostkey).length;
        if (this.size > Protocol.MAX_MESSAGE_SIZE) {
            throw new MessageSizeExceededException();
        }
    }

    public RSAPublicKey getHostkey() {
        return hostkey;
    }

    @Override
    public void send(ByteBuffer out) {
        byte[] enc;
        super.send(out);
        enc = SecurityHelper.encodeRSAPublicKey(hostkey);
        out.put(enc);
    }

    public static HelloMessage parse(ByteBuffer buf) throws MessageParserException {
        byte[] enc;
        HelloMessage message;
        RSAPublicKey hostkey;

        if (0 == buf.remaining()) {
            throw new MessageParserException("Encoded hostkey not present");
        }
        enc = new byte[buf.remaining()];
        buf.get(enc);
        try {
            hostkey = SecurityHelper.getRSAPublicKeyFromEncoding(enc);
        } catch (InvalidKeyException ex) {
            throw new MessageParserException("Invalid encoding for a RSA public key");
        }
        try {
            message = new HelloMessage(hostkey);
        } catch (MessageSizeExceededException ex) {
            throw new RuntimeException("This is a bug; please report");
        }
        return message;
    }

}
