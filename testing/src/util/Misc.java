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
package util;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.interfaces.RSAPublicKey;
import protocol.MessageParserException;

/**
 * Collection for miscellaneous utility functions. Instances of this class are
 * not possible.
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public abstract class Misc {

    public static InetSocketAddress fromAddressString(String address) throws
            URISyntaxException {
        URI uri;
        uri = new URI("gossip://" + address);
        String hostname = uri.getHost();
        int port = uri.getPort();
        return new InetSocketAddress(hostname, port);
    }

    public static RSAPublicKey parseKey(ByteBuffer buffer, int offset) throws
            MessageParserException {
        RSAPublicKey key;
        byte[] keyBytes;
        buffer.mark();
        try {
            buffer.position(buffer.position() + offset);
            keyBytes = new byte[buffer.remaining()];
            buffer.get(keyBytes);
            try {
                key = SecurityHelper.getRSAPublicKeyFromEncoding(keyBytes);
            } catch (InvalidKeyException ex) {
                throw new MessageParserException(ex.toString());
            }
        } finally {
            buffer.reset();
        }
        return key;
    }
}
