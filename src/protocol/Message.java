/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package protocol;

import java.io.IOException;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import protocol.Protocol.MessageType;
import protocol.dht.*;

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

    public static Message parseMessage (final ByteBuffer buf){
        int size;
        MessageType type;

        size = buf.getShort();
        type = MessageType.asMessageType(buf.getShort());
        switch(type) {
            case DHT_GET:
            case DHT_PUT:
            case DHT_TRACE:
            case DHT_GET_REPLY:
            case DHT_TRACE_REPLY:
                return DhtMessage.parse(buf, type);
            case KX_TN_BUILD_IN:
            case KX_TN_BUILD_OUT:
            case KX_TN_DESTROY:
            case KX_TN_READY:
                assert (false);
            default:
                assert (false);
        }
        return null;
    }
}
