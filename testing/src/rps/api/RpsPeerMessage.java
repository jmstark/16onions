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
package rps.api;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.interfaces.RSAPublicKey;
import java.util.Objects;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;
import util.Misc;
import util.SecurityHelper;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class RpsPeerMessage extends RpsApiMessage {

    private final InetSocketAddress address;
    private final RSAPublicKey hostkey;
    private final byte[] addressBytes;
    private final byte[] encoding;

    public RpsPeerMessage(InetSocketAddress address, RSAPublicKey hostkey)
            throws MessageSizeExceededException {

        this.addHeader(Protocol.MessageType.API_RPS_PEER);
        addressBytes = address.getAddress().getAddress();
        this.size += addressBytes.length;
        this.size += 2; //for port
        this.size += 2; //reserved
        this.address = address;
        encoding = SecurityHelper.encodeRSAPublicKey(hostkey);
        if ((this.size + encoding.length) > Protocol.MAX_MESSAGE_SIZE) {
            throw new MessageSizeExceededException("Given Hostkey is too long");
        }
        this.size += encoding.length;
        this.hostkey = hostkey;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public RSAPublicKey getHostkey() {
        return hostkey;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 43 * hash + Objects.hashCode(this.address);
        hash = 43 * hash + Objects.hashCode(this.hostkey);
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
        final RpsPeerMessage other = (RpsPeerMessage) obj;
        if (!Objects.equals(this.address, other.address)) {
            return false;
        }
        if (!Objects.equals(this.hostkey, other.hostkey)) {
            return false;
        }
        return true;
    }

    @Override
    public void send(ByteBuffer out) {
        super.send(out);
        out.putShort((short) this.address.getPort());
        if (4 == addressBytes.length) { //0 for IPv4; 1 for IPv6
            this.sendEmptyBytes(out, 2);
        } else {
            out.putShort((short) 1);
        }
        out.put(addressBytes);
        out.put(encoding);
    }


    public static RpsPeerMessage parse(ByteBuffer buffer) throws
            MessageParserException {
        RpsPeerMessage message;
        InetSocketAddress address;
        RSAPublicKey hostkey;
        int port;
        byte[] addressBytes;
        boolean isIPv4;
        short flags;

        int minSize = 8
                + 4 //ipv4
                + 16; //RSA key
        if (buffer.remaining() < minSize) {
            throw new MessageParserException(
                    "Message size too small for RPS Peer message");
        }
        port = buffer.getShort();
        flags = buffer.getShort();
        isIPv4 = (0 == (flags << 15)); //0 for IPv4; 1 for IPv6
        if (isIPv4) {
            addressBytes = new byte[4];
        } else {
            addressBytes = new byte[16];
        }
        hostkey = Misc.parseKey(buffer, addressBytes.length);
        buffer.get(addressBytes);
        try {
            address = new InetSocketAddress(InetAddress.getByAddress(
                    addressBytes),
                    port);
        } catch (UnknownHostException ex) {
            throw new MessageParserException("Invalid IP address");
        }
        try {
            message = new RpsPeerMessage(address, hostkey);
        } catch (MessageSizeExceededException ex) {
            throw new MessageParserException(ex.toString());
        }
        return message;
    }
}
