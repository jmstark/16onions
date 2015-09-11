/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package protocol.kx;

import java.nio.ByteBuffer;
import protocol.Hop;
import protocol.Message;
import protocol.Protocol;
import protocol.Protocol.MessageType;

/**
 *
 * @author troll
 */
public abstract class KxTunnelBuildMessage extends Message {
    private final byte nhops;
    private final byte[] pseudoID;
    private final Hop exchangePoint;

    KxTunnelBuildMessage(byte nhops, byte[] pseudoID, Hop exchangePoint){
        //this.addHeader(Protocol.MessageType.KX_TN_BUILD_IN);
        assert (null != exchangePoint);
        this.nhops = nhops;
        this.size += 4; // nhtops + 3 reserved
        assert (Protocol.IDENTITY_LENGTH == pseudoID.length);
        this.pseudoID = pseudoID;
        this.size += Protocol.IDENTITY_LENGTH;
        this.exchangePoint = exchangePoint;
        this.size += Hop.WIRE_SIZE;
    }

    @Override
    public void send (ByteBuffer out) {
        super.send(out);
        out.put(getNhops());
        this.sendEmptyBytes(out, 3); //3 bytes reserved
        out.put(getPseudoID());
        getExchangePoint().serialize(out);
    }

    /**
     * @return the nhops
     */
    public byte getNhops() {
        return nhops;
    }

    /**
     * @return the pseudoID
     */
    public byte[] getPseudoID() {
        return pseudoID;
    }

    /**
     * @return the exchangePoint
     */
    public Hop getExchangePoint() {
        return exchangePoint;
    }

    static final public KxTunnelBuildMessage parse (final ByteBuffer buf,
            MessageType type) {
        byte nhops;
        byte[] pseudoID;
        Hop exchangePoint;
        nhops = (byte) buf.get();
        buf.position(buf.position() + 3); //skip reserved portion
        pseudoID = new byte[Protocol.IDENTITY_LENGTH];
        buf.get(pseudoID);
        exchangePoint = Hop.parse(buf);
        switch (type) {
            case KX_TN_BUILD_IN:
                return new KxTunnelBuildIncomingMessage (nhops, pseudoID, exchangePoint);
            case KX_TN_BUILD_OUT:
                return new KxTunnelBuildOutgoingMessage (nhops, pseudoID, exchangePoint);

        }
        assert (false);
        return null;
    }
}
