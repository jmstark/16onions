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
package mockups.rps;

import gossip.api.AnnounceMessage;
import gossip.api.NotificationMessage;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import protocol.Message;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import util.SecurityHelper;

/**
 *
 * @author totakura
 */
public class MembershipMessage {

    private final byte[] keyEncoding; //encoding of the public key
    private final InetSocketAddress address;
    private final int size;
    private final boolean isIPv6; //0 for IPv4; 1 for IPv6
    public static final int DATATYPE = 8000;
    public static final short TTL = 255;

    public MembershipMessage(RSAPublicKey key, InetSocketAddress address) {
        this.address = address;
        int size = 0;
        size += 8; //timestamp
        size += 1; //ipv4/v6 flag
        size += 2; //port
        int addrSize = address.getAddress().getAddress().length;
        if (4 == addrSize) {
            this.isIPv6 = false;
        } else if (16 == addrSize) {
            this.isIPv6 = true;
        } else {
            throw new RuntimeException("Only IP network addresses are supported");
        }
        size += addrSize;
        this.keyEncoding = SecurityHelper.encodeRSAPublicKey(key);
        size += this.keyEncoding.length;
        this.size = size;
    }

    public byte[] serialize() {
        long timestamp = new Date().getTime();
        /**
         * [timestamp: 8 bytes; IPv4/v6: 1 byte; port: 2bytes; network address:
         * 4/16 bytes; key: variable
         */
        ByteBuffer buf = ByteBuffer.allocate(this.size);
        buf.putLong(timestamp);
        buf.put((byte) (isIPv6 ? 1 : 0));
        buf.putShort((short) address.getPort());
        buf.put(address.getAddress().getAddress());
        buf.put(keyEncoding);
        buf.flip();
        return buf.array();
    }

    public AnnounceMessage encapsulateAsAnnounce() throws MessageSizeExceededException {
        return new AnnounceMessage(TTL, DATATYPE, serialize());
    }

    public byte[] getKeyEncoding() {
        return keyEncoding;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public static MembershipMessage parseFromNotification(NotificationMessage message) throws MessageParserException {
        byte[] payload = message.getData();
        int datatype = message.getDatatype();
        if (DATATYPE != datatype) {
            throw new MessageParserException("Not a membership message");
        }
        boolean isIPv6;
        ByteBuffer buf;
        long timestamp;
        byte[] addrBytes;
        byte[] encoding;
        RSAPublicKey key;
        int port;

        buf = ByteBuffer.wrap(payload);
        timestamp = buf.getLong();
        isIPv6 = (buf.get() == 1);
        port = Message.unsignedIntFromShort(buf.getShort());
        addrBytes = new byte[isIPv6 ? 16 : 4];
        buf.get(addrBytes);
        encoding = new byte[buf.remaining()];
        buf.get(encoding);
        try {
            key = SecurityHelper.getRSAPublicKeyFromEncoding(encoding);
        } catch (InvalidKeyException ex) {
            throw new MessageParserException("RSA public key cannot be parsed");
        }
        try {
            return new MembershipMessage(key, new InetSocketAddress(InetAddress.getByAddress(addrBytes), port));
        } catch (UnknownHostException ex) {
            throw new MessageParserException("Invalid IP address given");
        }
    }
}
