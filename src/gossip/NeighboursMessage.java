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
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;

/**
 * Message to spread our neighbors addresses to the other peer
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
class NeighboursMessage extends PeerMessage {

    protected LinkedList<Peer> peers;

    NeighboursMessage(Peer peer) throws MessageSizeExceededException {
        super();
        this.peers = new LinkedList();
        this.addHeader(Protocol.MessageType.GOSSIP_NEIGHBORS);
        this.size += 2; //field for holding the number of peers in this message
        this.addNeighbour(peer);
    }

    final void addNeighbour(Peer peer) throws MessageSizeExceededException {
        // field for holding number of addresses; currently we hardcode this to 1
        int requiredSize = 2;
        requiredSize += this.sizeFor(peer.getAddress());
        if (requiredSize + this.size > Protocol.MAX_MESSAGE_SIZE) {
            throw new MessageSizeExceededException();
        }
        this.size += requiredSize;
        peers.add(peer);
    }

    Iterator<Peer> getPeersAsIterator() {
        return peers.iterator();
    }

    private int sizeFor(InetSocketAddress sock_address) {
        InetAddress address = sock_address.getAddress();
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
        return addr_size;
    }

    private static void sendPeerAddresses(ByteBuffer out, Peer peer) {
        out.putShort((short) 1);
        InetSocketAddress sock_address = peer.getAddress();
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

    @Override
    public void send(ByteBuffer out) {
        super.send(out);
        out.putShort((short) this.peers.size());
        for (Peer peer : this.peers) {
            sendPeerAddresses(out, peer);
        }
    }

    protected static NeighboursMessage parse(ByteBuffer buf)
            throws MessageParserException {
        NeighboursMessage message;
        InetSocketAddress sock_address;
        InetAddress address;
        Peer peer;
        byte[] addr_bytes;
        short port;
        short addr_size;
        short peer_count;
        short address_count;

        message = null;
        peer_count = buf.getShort();
        // each peer will occupy at minimum
        // 2 (counter for address) + 2 (size) + 2(port) + 4 (ipv4) = 10
        if ((peer_count * 10) > buf.remaining()) {
            throw new MessageParserException();
        }
        try {
            for (; 0 < peer_count; peer_count--) {
                address_count = buf.getShort();
                if (1 != address_count) {
                    throw new MessageParserException("Current version expects only one address per peer");
                }
                //each address will occupy at minimum
                //2(size) + 2(port) + 4(ipv4) = 8
                if ((address_count * 8) > buf.remaining()) {
                    throw new MessageParserException();
                }
                for (; 0 < address_count; address_count--) {
                    addr_size = buf.getShort();
                    switch (addr_size) {
                        case 8:
                            addr_bytes = new byte[4];
                            break;
                        case 20:
                            addr_bytes = new byte[16];
                            break;
                        default:
                            throw new MessageParserException("Invalid size for address: "
                                    + addr_size + " bytes");
                    }
                    port = buf.getShort();
                    buf.get(addr_bytes);
                    try {
                        address = InetAddress.getByAddress(addr_bytes);
                    } catch (UnknownHostException unknown) {
                        throw new RuntimeException("Control flow error; please report");
                    }
                    sock_address = new InetSocketAddress(address, port);
                    peer = new Peer(sock_address);
                    try {
                        if (null == message) {
                            message = new NeighboursMessage(peer);
                        } else {
                            message.addNeighbour(peer);
                        }
                    } catch (MessageSizeExceededException ex) {
                        throw new RuntimeException("Control flow error; please report");
                    }
                }
            }
        } catch (BufferUnderflowException underflow) {
            throw new MessageParserException();
        }
        if (null == message) {
            throw new MessageParserException("Invalid HelloMessage with 0 peers");
        }
        return message;
    }
}
