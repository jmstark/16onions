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
package onion.api;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import protocol.Message;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;
import util.SecurityHelper;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class OnionTunnelReadyMessage extends OnionApiMessage {

    private final byte[] encodedKey;
    private final long id;

    public OnionTunnelReadyMessage(long id, byte[] encodedKey) throws
            MessageSizeExceededException {
        this.addHeader(Protocol.MessageType.API_ONION_TUNNEL_READY);
        this.id = id;
        this.size += 4; //tunnel id
        this.encodedKey = encodedKey;
        this.size += encodedKey.length;
        if (size > Protocol.MAX_MESSAGE_SIZE) {
            throw new MessageSizeExceededException();
        }
    }

    public byte[] getEncodedKey() {
        return encodedKey;
    }

    public long getId() {
        return id;
    }

    @Override
    public void send(ByteBuffer out) {
        super.send(out);
        out.putInt((int) id);
        out.put(encodedKey);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + Arrays.hashCode(this.encodedKey);
        hash = 83 * hash + (int) (this.id ^ (this.id >>> 32));
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
        final OnionTunnelReadyMessage other = (OnionTunnelReadyMessage) obj;
        if (this.id != other.id) {
            return false;
        }
        if (!Arrays.equals(this.encodedKey, other.encodedKey)) {
            return false;
        }
        return true;
    }

    public static OnionTunnelReadyMessage parse(ByteBuffer buffer) throws
            MessageParserException {
        long id;
        byte[] encoding;
        RSAPublicKey key;
        OnionTunnelReadyMessage message;

        id = Message.unsignedLongFromInt(buffer.getInt());
        encoding = new byte[buffer.remaining()];
        buffer.get(encoding);
        try {
            key = SecurityHelper.getRSAPublicKeyFromEncoding(encoding);
        } catch (InvalidKeyException ex) {
            throw new MessageParserException(
                    "Invalid DER format for RSA Public Key");
        }
        try {
            message = new OnionTunnelReadyMessage(id, encoding);
        } catch (MessageSizeExceededException ex) {
            throw new RuntimeException("This is a bug; please report.");
        }
        return message;
    }
}
