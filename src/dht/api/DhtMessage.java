/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dht.api;

import java.nio.ByteBuffer;
import protocol.Message;
import protocol.Protocol.MessageType;

/**
 *
 * @author troll
 */
public abstract class DhtMessage extends Message {

    private DHTKey key;
    private boolean keyAdded;

    public void addKey(DHTKey key) {
        assert (!this.keyAdded);
        this.keyAdded = true;
        this.key = key;
        this.size += key.getValue().length;
    }

    @Override
    protected void send(ByteBuffer out) {
        assert (this.keyAdded);
        super.send(out);
        out.put(key.getValue());
    }

    static public DhtMessage parse(final ByteBuffer buf, MessageType type) {
        assert (buf.remaining() >= DHT_KEY_SIZE);
        byte[] keyData = new byte[DHT_KEY_SIZE];
        buf.get(keyData);
        DHTKey key = new DHTKey(keyData);
        switch (type) {
            case DHT_GET:
                return DhtGetMessage.parse(buf, key);
            case DHT_PUT:
                return DhtPutMessage.parse(buf, key);
            case DHT_TRACE:
                return DhtTraceMessage.parse(buf, key);
            case DHT_GET_REPLY:
                return DhtGetReplyMessage.parse(buf, key);
            case DHT_TRACE_REPLY:
                return DhtTraceReplyMessage.parse(buf, key);
            default:
                assert (false);
        }
        return null;
    }

    static final int DHT_KEY_SIZE = 32;

    /**
     * @return the key
     */
    public DHTKey getKey() {
        return key;
    }
}
