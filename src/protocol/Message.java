/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package protocol;

import java.io.IOException;
import java.io.DataOutputStream;
import protocol.Protocol.MessageType;

/**
 *
 * @author troll
 */
public abstract class Message {
    protected int size;
    
    private boolean headerAdded;
    private MessageType type;
    
    protected Message(){    
        this.size = 0;
        this.headerAdded = false;
    }
    
    protected final void addHeader(MessageType type){        
        
        assert (!this.headerAdded);
        this.headerAdded = true;
        this.type = type;
        this.size += 4;
    }

    protected void send(DataOutputStream out) throws IOException {
        assert (this.headerAdded);
        out.writeShort(this.size);
        out.writeShort(this.type.getNumVal());
    }
}
