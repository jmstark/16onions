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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.interfaces.RSAPublicKey;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;
import util.Misc;
import util.SecurityHelper;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class OnionTunnelBuildMessage extends OnionApiMessage {

    private final InetSocketAddress address;
    private final RSAPublicKey key;
    private final byte[] encoding;

    public OnionTunnelBuildMessage(InetSocketAddress address, RSAPublicKey key)
            throws MessageSizeExceededException {
        this.addHeader(Protocol.MessageType.API_ONION_TUNNEL_BUILD);
        this.address = address;
        size += address.getAddress().getAddress().length;
        size += 4;//2 for reserved;2 for port
        this.key = key;
        this.encoding = SecurityHelper.encodeRSAPublicKey(key);
        size += this.encoding.length;
        if (size > Protocol.MAX_MESSAGE_SIZE) {
            throw new MessageSizeExceededException();
        }
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public RSAPublicKey getKey() {
        return key;
    }

    public byte[] getEncoding() {
        return encoding;
    }

    @Override
    public void send(ByteBuffer out) {
        super.send(out);
        super.sendEmptyBytes(out, 2); //reserved
        out.putShort((short) this.address.getPort());
        out.put(address.getAddress().getAddress());
        out.put(encoding);
    }

    public static OnionTunnelBuildMessage parse(ByteBuffer buffer) throws
            MessageParserException {
        OnionTunnelBuildMessage message;
        InetSocketAddress address;
        RSAPublicKey hostkey;
        int port;
        byte[] addressBytes;

        int minSize = 8
                + 4 //ipv4
                + 16; //RSA key
        if (buffer.remaining() < minSize) {
            throw new MessageParserException(
                    "Message size too small for RPS Peer message");
        }
        buffer.getShort();//ignore reserved field
        port = buffer.getShort();
        //this is bad hack
        //assume address is 4 bytes
        try {
            addressBytes = new byte[4];
            hostkey = Misc.parseKey(buffer, addressBytes.length);
        } catch (MessageParserException ex) {
            addressBytes = new byte[16];
            hostkey = Misc.parseKey(buffer, addressBytes.length);
        }
        buffer.get(addressBytes);
        try {
            address = new InetSocketAddress(InetAddress.getByAddress(
                    addressBytes),
                    port);
        } catch (UnknownHostException ex) {
            throw new MessageParserException("Invalid IP address");
        }
        try {
            message = new OnionTunnelBuildMessage(address, hostkey);
        } catch (MessageSizeExceededException ex) {
            throw new MessageParserException(ex.toString());
        }
        return message;
    }
}
