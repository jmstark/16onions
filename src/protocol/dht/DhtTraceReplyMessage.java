/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package protocol.dht;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.ListIterator;
import protocol.Hop;

/**
 *
 * @author troll
 */
public class DhtTraceReplyMessage extends DhtMessage {
    private LinkedList<Hop> hops;
    
    public DhtTraceReplyMessage(byte[] key){
        this.addKey(key);
    }            
    
    public void addHop(Hop hop){
        hops.add(hop);
        this.size += Hop.WIRE_SIZE;
    }
    
    @Override
    public void send(DataOutputStream out) throws IOException {
        Hop hop;
        super.send(out);
        ListIterator<Hop> iter = hops.listIterator();
        while (iter.hasNext()){
            hop = iter.next();
            hop.serialize (out);
        }
    }
}
