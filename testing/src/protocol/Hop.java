/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package protocol;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 *
 * @author Emertat
 */
public final class Hop {

    private final byte[] ID, IPv4, IPv6;
    private final int KX_port;

    Hop(byte[] ID, byte[] IPv4, byte[] IPv6, int KX_port) {
        assert (Protocol.IDENTITY_LENGTH == ID.length);
        assert (4 == IPv4.length);
        assert (16 == IPv6.length);
        assert (0 < KX_port);
        assert (KX_port <= 65535);
        this.ID = ID;
        this.IPv4 = IPv4;
        this.IPv6 = IPv6;
        this.KX_port = KX_port;
    }

    public byte[] getID() {
        return ID;
    }

    public byte[] getIPv4() {
        return IPv4;
    }

    public byte[] getIPv6() {
        return IPv6;
    }

    public int getKX_port() {
        return KX_port;
    }

    @Override
    public String toString() {
        StringBuilder strBuilder = new StringBuilder(256);
        try {
            if (null != this.IPv4) {
                strBuilder.append(InetAddress.getByAddress(this.IPv4).toString());
                strBuilder.append('/');
            }
            if (null != this.IPv6) {
                strBuilder.append(InetAddress.getByAddress(this.IPv6).toString());
            }
        } catch (UnknownHostException e) {
            assert (false);
        }
        strBuilder.append(':');
        strBuilder.append(this.KX_port);
        return strBuilder.toString();
    }

    public void serialize(ByteBuffer out) {
        out.put(this.ID);
        out.putShort((short) this.KX_port);
        out.putShort((short) 0);//reserved
        out.put(this.IPv4);
        out.put(this.IPv6);
    }

    //ID: 32; KX port + reserved: 4; IPv4: 4; IPv6: 16
    public static final int WIRE_SIZE = 32 + 4 + 4 + 16;

    public static Hop parse(final ByteBuffer buf) {
        byte[] ID, ipv4, ipv6;
        int port;
        port = Message.getUnsignedShort(buf);
        buf.position(buf.position() + 2); //skip 2 reserved bytes
        ID = new byte[Protocol.IDENTITY_LENGTH];
        buf.get(ID);
        ipv4 = new byte[4];
        ipv6 = new byte[16];
        buf.get(ipv4);
        buf.get(ipv6);
        return new Hop(ID, ipv4, ipv6, port);
    }
}
