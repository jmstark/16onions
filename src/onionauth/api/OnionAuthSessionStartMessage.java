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
package onionauth.api;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;
import util.SecurityHelper;

/**
 *
 * @author totakura
 */
public class OnionAuthSessionStartMessage extends OnionAuthApiMessage {

    private final byte[] keyEnc;
    private final RSAPublicKey pkey;

    /**
     * Return new OnionAuthSessionStartMessage.
     *
     * @param pkey the public key
     * @throws protocol.MessageSizeExceededException
     */
    public OnionAuthSessionStartMessage(RSAPublicKey pkey)
            throws MessageSizeExceededException {
        this.pkey = pkey;
        this.keyEnc = SecurityHelper.encodeRSAPublicKey(pkey);
        this.addHeader(Protocol.MessageType.API_AUTH_SESSION_START);
        if ((this.size + this.keyEnc.length) > Protocol.MAX_MESSAGE_SIZE) {
            throw new MessageSizeExceededException();
        }
        this.size += this.keyEnc.length;
    }

    public byte[] getKeyEnc() {
        return keyEnc;
    }

    public RSAPublicKey getPKey() {
        return pkey;
    }

    @Override
    public void send(ByteBuffer out) {
        super.send(out);
        out.put(keyEnc);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + Arrays.hashCode(this.keyEnc);
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
        final OnionAuthSessionStartMessage other = (OnionAuthSessionStartMessage) obj;
        if (!Arrays.equals(this.keyEnc, other.keyEnc)) {
            return false;
        }
        return true;
    }

    public static OnionAuthSessionStartMessage parse(ByteBuffer buf)
            throws MessageParserException {
        byte[] enc;
        OnionAuthSessionStartMessage message;
        RSAPublicKey pkey;

        enc = new byte[buf.remaining()];
        buf.get(enc);
        try {
            pkey = SecurityHelper.getRSAPublicKeyFromEncoding(enc);
        } catch (InvalidKeyException ex) {
            throw new MessageParserException("Invalid key");
        }
        try {
            message = new OnionAuthSessionStartMessage(pkey);
        } catch (MessageSizeExceededException ex) {
            throw new MessageParserException("Size exceeded");
        }
        return message;
    }

}
