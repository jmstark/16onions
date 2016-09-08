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
public class DhtPutMessage extends DhtMessage {

    private final int ttl;
    private final byte replication;
    private final DHTContent content;

    /**
     * Create a DHT PUT message
     * @param key
     * @param ttl time to live value;  This cannot be a negative number and
     *              should be less than 65536.
     * @param replication
     * @param content
     */
    public DhtPutMessage(DHTKey key, int ttl, byte replication, DHTContent content) {
        assert (0 <= ttl);
        assert (ttl <= 65535);
        this.ttl = ttl;
        this.replication = replication;
        this.size += 8; //ttl + replication + reserved
        this.content = content;
        this.size += content.getValue().length;
        this.addKey(key);
        this.addHeader(Protocol.MessageType.DHT_PUT);
    }

    @Override
    public void send(ByteBuffer out) {
        super.send(out);
        out.putShort((short) this.ttl);
        out.put(this.replication);
        out.put(new byte[5]); //reserved dummy
        out.put(this.content.getValue());
    }

    static public DhtPutMessage parse(final ByteBuffer buf, DHTKey key) {
        int ttl = getUnsignedShort(buf);
        byte replication = buf.get();
        byte reserved1 = buf.get();// skip
        int reserved2 = buf.getInt();
        byte[] content = new byte[buf.remaining()];
        buf.get(content);
        return new DhtPutMessage(key, ttl, replication, new DHTContent(content));
    }

    /**
     * @return the TTL
     */
    public int getTTL() {
        return ttl;
    }

    /**
     * @return the replication
     */
    public byte getReplication() {
        return replication;
    }

    /**
     * @return the content
     */
    public DHTContent getContent() {
        return content;
    }
}
