/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package protocol.kx;

import java.nio.ByteBuffer;
import protocol.Message;
import protocol.Protocol;
/**
 *
 * @author troll
 */
public class KxTunnelDestroyMessage extends Message{
 final byte[] pseudoID;

    public KxTunnelDestroyMessage(byte[] pseudoID) {
        assert (Protocol.IDENTITY_LENGTH == pseudoID.length);
        this.pseudoID = pseudoID;
        this.size += Protocol.IDENTITY_LENGTH;
        this.addHeader(Protocol.MessageType.KX_TN_DESTROY);
    }

    byte[] getPseudoID () {
        return this.pseudoID;
    }

    @Override
    public void send(ByteBuffer out) {
        super.send(out);
        out.put(pseudoID);
    }

    public static KxTunnelDestroyMessage parse (ByteBuffer in) {
        byte[] id;
        id = new byte[Protocol.IDENTITY_LENGTH];
        in.get(id);
        return new KxTunnelDestroyMessage (id);
    }
}
