/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gossip;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArraySet;
import protocol.Connection;
import protocol.Message;

/**
 *
 * @author totakura
 */
final class Peer {

    static final int DEFAULT_ADDRESSES = 3;
    private CopyOnWriteArraySet<InetSocketAddress> addresses;
    private Connection connection;

    Peer(InetSocketAddress address) {
        this(address, null);
    }

    Peer(InetSocketAddress address, Connection connection) {
        this.connection = connection;
        this.addresses = new CopyOnWriteArraySet();
        this.addresses.add(address);
    }

    void addAddress(InetSocketAddress address) {
        this.addresses.add(address);
    }

    void addAddressesFromIterator(Iterator<InetSocketAddress> iterator) {
        while (iterator.hasNext()) {
            this.addresses.add(iterator.next());
        }
    }

    Iterator<InetSocketAddress> getAddressIterator() {
        return addresses.iterator();
    }

    int getAddressCount() {
        return this.addresses.size();
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

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    void sendMessage(Message message) {
        this.connection.sendMsg(message);
    }
}
