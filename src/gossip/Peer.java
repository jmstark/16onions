/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gossip;

import java.net.InetSocketAddress;
import java.util.ArrayList;

/**
 *
 * @author totakura
 */
public class Peer {
    private ArrayList<InetSocketAddress> addresses;

    public Peer(ArrayList<InetSocketAddress> addresses) {
        this.addresses = addresses;
    }
}

