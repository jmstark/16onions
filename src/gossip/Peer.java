/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gossip;

import java.net.InetSocketAddress;
import protocol.Connection;
import protocol.Message;

/**
 *
 * @author totakura
 */
final class Peer {
    private Connection connection;
    private InetSocketAddress address;

    Peer(InetSocketAddress address) {
        this(address, null);
    }

    Peer(InetSocketAddress address, Connection connection) {
        this.connection = connection;
        this.address = address;
    }

    InetSocketAddress getAddress() {
        return address;
    }

    /**
     * Return the connection status of this peer.
     *
     * @return true if we have an active connection to this peer; false if not
     */
    boolean isConnected() {
        if (null == this.connection) {
            return false;
        }
        return this.connection.getChannel().isOpen();
    }

    void setConnection(Connection connection) {
        this.connection = connection;
    }

    void sendMessage(Message message) {
        this.connection.sendMsg(message);
    }

    void disconnect() {
        this.connection.disconnect();
    }
}
