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

    HelloMessage(InetSocketAddress sock_address) {
        super();
        this.addHeader(Protocol.MessageType.GOSSIP_HELLO);
        this.addresses = new ArrayList(Peer.DEFAULT_ADDRESSES);
        this.size += 2; //counter for addresses
        try {
            this.addAddress(sock_address, this);
        } catch (MessageSizeExceededException ex) {
            throw new RuntimeException("this should not happen; please report it");
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

    }
}
