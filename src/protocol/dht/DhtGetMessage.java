/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package protocol.dht;

import java.nio.ByteBuffer;
import protocol.Protocol;

/**
 *
 * @author troll
 */
public class DhtGetMessage extends DhtMessage {
    public DhtGetMessage(DHTKey key){
        this.addKey(key);
        this.addHeader(Protocol.MessageType.DHT_GET);
    }

    @Override
    public void send (ByteBuffer out) {
        super.send(out);
    }

    static public DhtGetMessage parse (final ByteBuffer buf, DHTKey key) {
        return new DhtGetMessage (key);
    }
}
