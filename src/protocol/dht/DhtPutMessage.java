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
public class DhtPutMessage extends DhtMessage {

    private final short ttl;
    private final byte replication;
    private final DHTContent content;

    public DhtPutMessage(DHTKey key, short ttl, byte replication, DHTContent content) {
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
        out.putShort(this.ttl);
        out.put(this.replication);
        out.put(new byte[5]); //reserved dummy
        out.put(this.content.getValue());
    }

    static public DhtPutMessage parse(final ByteBuffer buf, DHTKey key) {
        short ttl = buf.getShort();
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
    public short getTTL() {
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
