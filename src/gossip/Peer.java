/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gossip;

import java.net.InetSocketAddress;
import java.util.Objects;
import protocol.Connection;
import protocol.Message;

/**
 *
 * @author totakura
 */
public final class Peer {
    private Connection connection;
    private InetSocketAddress address;

    public Peer(InetSocketAddress address) {
        this(address, null);
    }

    public Peer(InetSocketAddress address, Connection connection) {
        this.connection = connection;
        this.address = address;
    }

    public void setAddress(InetSocketAddress address) {
        this.address = address;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    /**
     * Return the connection status of this peer.
     *
     * @return true if we have an active connection to this peer; false if not
     */
    public boolean isConnected() {
        if (null == this.connection) {
            return false;
        }
        return this.connection.getChannel().isOpen();
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public void sendMessage(Message message) {
        this.connection.sendMsg(message);
    }

    public void disconnect() {
        this.connection.disconnect();
        this.connection = null;
    }

    @Override
    public String toString() {
        if (null != address) {
            return String.format("Peer@[%s]:%d",
                    address.getHostString(), address.getPort());
        }
        return super.toString();
    }

    /**
     * Compare if given object is same as this peer.
     *
     * We term two Peer objects as equal if they have the same socket address.
     *
     * @param obj the other object to compare
     * @return true if the peer object has the same socket address; false if not
     */
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
        final Peer other = (Peer) obj;
        if (!Objects.equals(this.address, other.address)) {
            return false;
        }
        return true;
    }
}
