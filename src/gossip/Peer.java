/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gossip;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import protocol.Connection;
import protocol.Message;

/**
 *
 * @author totakura
 */
public class Peer {

    static final int DEFAULT_ADDRESSES = 3;
    private ArrayList<InetSocketAddress> addresses;
    private final Connection connection;

    public Peer(InetSocketAddress address, Connection connection) {
        this.connection = connection;
        this.addresses = new ArrayList(DEFAULT_ADDRESSES);
        this.addresses.add(address);
    }

    public void addAddress(InetSocketAddress address) {
        this.addresses.add(address);
    }

    public void addAddressesFromIterator(Iterator<InetSocketAddress> iterator) {
        while (iterator.hasNext()) {
            this.addresses.add(iterator.next());
        }
    }

    public void sendMessage(Message message) {
        this.connection.sendMsg(message);
    }
}
