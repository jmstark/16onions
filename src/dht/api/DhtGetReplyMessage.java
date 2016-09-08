/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dht.api;

import java.nio.ByteBuffer;

/**
 *
 * @author troll
 */
public class DhtGetReplyMessage extends DhtMessage {

    private final DHTContent content;

    public DhtGetReplyMessage(DHTKey key, DHTContent content) {
        this.addKey(key);
        this.content = content;
        this.size += content.getValue().length;
    }

    @Override
    protected void send(ByteBuffer out) {
        super.send(out);
        out.put(content.getValue());
    }

    public static DhtGetReplyMessage parse(final ByteBuffer buf, DHTKey key) {
        byte[] content = new byte[buf.remaining()];
        buf.get(content);
        return new DhtGetReplyMessage(key, new DHTContent(content));
    }

    /**
     * @return the content
     */
    public DHTContent getContent() {
        return content;
    }
}
