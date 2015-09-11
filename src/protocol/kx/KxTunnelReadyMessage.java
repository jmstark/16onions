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
public class KxTunnelReadyMessage extends Message {
    final byte[] pseudoID;
    final byte[] ipv4;
    final byte[] ipv6;

    public KxTunnelReadyMessage (byte[] pseudoID, byte[] ipv4, byte[] ipv6) {
        assert (Protocol.IDENTITY_LENGTH == pseudoID.length);
        assert (4 == ipv4.length);
        assert (16 == ipv6.length);
        this.pseudoID = pseudoID;
        this.size += Protocol.IDENTITY_LENGTH;
        this.size += 4;// reserved 4 bytes
        this.ipv4 = ipv4;
        this.size += 4;
        this.ipv6 = ipv6;
        this.size += 16;
        this.addHeader(Protocol.MessageType.KX_TN_READY);
    }

    byte[] getPseudoID () {
        return this.pseudoID;
    }

    @Override
    public void send(ByteBuffer out) {
        super.send(out);
        out.put(pseudoID);
        this.sendEmptyBytes(out, 4);
        out.put (ipv4);
        out.put (ipv6);
    }

    public static KxTunnelReadyMessage parse (ByteBuffer in) {
        byte[] id, ipv4, ipv6;
        id = new byte[Protocol.IDENTITY_LENGTH];
        ipv4 = new byte[4];
        ipv6 = new byte[16];
        in.get(id);
        in.position(in.position() + 4);
        in.get(ipv4);
        in.get(ipv6);
        return new KxTunnelReadyMessage (id, ipv4, ipv6);
    }
}
