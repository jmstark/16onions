/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dht.api;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.ListIterator;
import protocol.Hop;

/**
 *
 * @author troll
 */
public class DhtTraceReplyMessage extends DhtMessage {

    private LinkedList<Hop> hops;

    public DhtTraceReplyMessage(DHTKey key) {
        this.addKey(key);
    }

    public void addHop(Hop hop) {
        hops.add(hop);
        this.size += Hop.WIRE_SIZE;
    }

    @Override
    protected void send(ByteBuffer out) {
        Hop hop;
        super.send(out);
        ListIterator<Hop> iter = hops.listIterator();
        while (iter.hasNext()) {
            hop = iter.next();
            hop.serialize(out);
        }
    }

    public static DhtTraceReplyMessage parse(final ByteBuffer buf, DHTKey key) {
        DhtTraceReplyMessage msg = new DhtTraceReplyMessage(key);
        Hop hop;
        while (null != (hop = Hop.parse(buf))) {
            msg.addHop(hop);
        }
        return msg;
    }
}
