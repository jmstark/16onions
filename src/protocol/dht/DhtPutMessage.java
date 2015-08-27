/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package protocol.dht;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Stack;
import protocol.Protocol;

/**
 *
 * @author troll
 */
public class DhtPutMessage extends DhtMessage{
    private int ttl;
    private int replication;
    private byte[] content;
    
    public DhtPutMessage(byte[] key, int ttl, int replication, byte[] content){
        this.ttl = ttl;               
        this.replication = replication;
        this.size += 8; //ttl + replication + reserved
        this.content = content;
        this.size += content.length;
        this.addKey(key);
        this.addHeader(Protocol.MessageType.DHT_PUT);
    }
    
    @Override
    public void send(DataOutputStream out) throws IOException
    {
        super.send(out);
        out.writeShort(this.ttl);
        out.writeByte(this.replication);
        out.write(new byte[5]); //reserved dummy
        out.write(this.content);
    }
}
