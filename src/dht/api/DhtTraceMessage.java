/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dht.api;

import java.nio.ByteBuffer;
import protocol.Protocol;

/**
 *
 * @author troll
 */
public class DhtTraceMessage extends DhtMessage {

    public DhtTraceMessage(DHTKey key) {
        this.addKey(key);
        this.addHeader(Protocol.MessageType.DHT_TRACE);
    }

    @Override
    public void send(ByteBuffer out) {
        super.send(out);
    }

    public static DhtTraceMessage parse(final ByteBuffer buf, DHTKey key) {
        return new DhtTraceMessage(key);
    }
}
