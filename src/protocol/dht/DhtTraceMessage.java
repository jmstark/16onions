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
public class DhtTraceMessage extends DhtMessage {
    public DhtTraceMessage(byte[] key){
        this.addKey(key);
        this.addHeader(Protocol.MessageType.DHT_TRACE);
    }

    @Override
    public void send (ByteBuffer out) {
        super.send(out);
    }

    public static DhtTraceMessage parse (final ByteBuffer buf, byte[] key){
        return new DhtTraceMessage (key);
    }
}
