/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package protocol;

import java.nio.ByteBuffer;
import protocol.Protocol.MessageType;
import protocol.dht.DhtMessage;
import protocol.gossip.GossipMessage;
import protocol.kx.KxTunnelBuildMessage;
import protocol.kx.KxTunnelDestroyMessage;
import protocol.kx.KxTunnelReadyMessage;

/**
 *
 * @author troll
 */
public abstract class Message {

    protected int size;

    private boolean headerAdded;
    private MessageType type;

    protected Message() {
        this.size = 0;
        this.headerAdded = false;
    }

    protected final void addHeader(MessageType type) {

        assert (!this.headerAdded);
        this.headerAdded = true;
        this.type = type;
        this.size += 4;
    }

    protected final void changeMessageType(MessageType type) {
        assert (this.headerAdded);
        this.type = type;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Message)) {
            return false;
        }
        Message otherMsg = (Message) obj;
        if (otherMsg.getSize() != size) {
            return false;
        }
        return otherMsg.getType() == type;
    }

    protected void send(ByteBuffer out) {
        assert (this.headerAdded);
        out.putShort((short) this.size);
        out.putShort((short) this.type.getNumVal());
    }

    protected final void sendEmptyBytes(ByteBuffer out, int nbytes) {
        assert (0 < nbytes);
        byte[] zeros = new byte[nbytes];
        out.put(zeros);
    }

    /**
     * @return the size
     */
    public int getSize() {
        return size;
    }

    /**
     * @return the type
     */
    public MessageType getType() {
        return type;
    }
}
