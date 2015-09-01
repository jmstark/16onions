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
    public DhtGetMessage(byte[] key){
        this.addKey(key);
        this.addHeader(Protocol.MessageType.DHT_GET);
    }

    static public DhtGetMessage parse (final ByteBuffer buf, byte[] key) {
        return new DhtGetMessage (key);
    }
}
