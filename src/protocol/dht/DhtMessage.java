/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package protocol.dht;

import java.io.DataOutputStream;
import java.io.IOException;
import protocol.Message;

/**
 *
 * @author troll
 */
public abstract class DhtMessage extends Message {
    private byte[] key;
    private boolean keyAdded;
    
    public void addKey(byte[] key){
        assert (!this.keyAdded);
        this.keyAdded = true;
        this.key = key;
        this.size += key.length;        
    }
    
    @Override
    public void send(DataOutputStream out) throws IOException{
        assert (this.keyAdded);
        super.send(out);
        out.write(key);
    }
}
