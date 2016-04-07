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
package gossip;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
class HelloMessage extends PeerMessage {

    private final ArrayList<InetSocketAddress> addresses;

    HelloMessage() {
        this.addHeader(Protocol.MessageType.GOSSIP_HELLO);
        this.addresses = new ArrayList(Peer.DEFAULT_ADDRESSES);
    }

    void addAddress(InetSocketAddress sock_address) throws MessageSizeExceededException {
        InetAddress address = sock_address.getAddress();
        int port = sock_address.getPort();
        int addr_size = 0;
        addr_size += 2; //for size
        addr_size += 2; //for port
        if (address instanceof Inet4Address) {
            addr_size += 4;
        } else if (address instanceof Inet6Address) {
            addr_size += 16;
        } else {
            throw new RuntimeException("Unknown address class");
        }
        if ((this.size + addr_size) > Protocol.MAX_MESSAGE_SIZE) {
            throw new MessageSizeExceededException();
        }
        this.size += addr_size;
    }

    @Override
    public final void send(ByteBuffer out) {
        for (InetSocketAddress sock_address : this.addresses) {
            InetAddress address;
            byte[] addr_bytes;
            int port;
            int addr_size;
            addr_size = 4; // for `size' and `port'
            port = sock_address.getPort();
            address = sock_address.getAddress();
            if (address instanceof Inet4Address) {
                Inet4Address ipv4;
                ipv4 = (Inet4Address) address;
                addr_bytes = ipv4.getAddress();
            } else if (address instanceof Inet6Address) {
                Inet6Address ipv6;
                ipv6 = (Inet6Address) address;
                addr_bytes = ipv6.getAddress();
            } else {
                throw new RuntimeException("Unknown address class");
            }
            addr_size += addr_bytes.length;
            out.putShort((short) addr_size);
            out.putShort((short) port);
            out.put(addr_bytes);
        }
    }

    public final Iterator<InetSocketAddress> getAddressIterator() {
        return this.addresses.iterator();
    }

    /**
     * Parse the given buffer into a HelloMessage.
     *
     * @param buf the buffer to parse
     * @param size the size in the buffer to parse
     * @return the hello message
     */
    static HelloMessage parse(ByteBuffer buf, int size) throws MessageParserException {
        HelloMessage message;
        InetSocketAddress sock_address;
        InetAddress address;
        byte[] addr_bytes;
        int port;
        int addr_size;
        message = new HelloMessage();
        while (0 < size) {
            addr_size = buf.getShort();
            switch (addr_size) {
                case 8:
                    addr_bytes = new byte[4];
                    break;
                case 20:
                    addr_bytes = new byte[16];
                    break;
                default:
                    throw new MessageParserException("Invalid size for address: " + addr_size + " bytes");
            }
            port = buf.getShort();
            buf.get(addr_bytes);
            try {
                address = InetAddress.getByAddress(addr_bytes);
            } catch (UnknownHostException unknown) {
                throw new RuntimeException("Control flow error; please report");
            }
            sock_address = new InetSocketAddress(address, port);
            try {
                message.addAddress(sock_address);
            } catch (MessageSizeExceededException ex) {
                throw new RuntimeException("Control flow error; please report");
            }
        }
        if (message.addresses.isEmpty()) {
            throw new MessageParserException("Invalid HelloMessage with 0 addresses");
        }
        return message;
    }
}
