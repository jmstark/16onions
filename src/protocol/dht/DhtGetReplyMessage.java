/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package protocol.dht;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 *
 * @author troll
 */
public class DhtGetReplyMessage extends DhtMessage {

    private byte[] content;

    public DhtGetReplyMessage(byte[] key, byte[] content) {
        this.addKey(key);
        this.content = content;
    }

    @Override
    public void send(DataOutputStream out) throws IOException {
        super.send(out);
        out.write(content);
    }

    public static DhtGetReplyMessage parse (final ByteBuffer buf, byte[] key) {
        byte[] content = new byte[buf.remaining()];
        buf.get(content);
        return new DhtGetReplyMessage (key, content);
    }
}
