/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package protocol;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 *
 * @author Emertat
 */
public class Hop {

    private byte[] ID, IPv4, IPv6;
    private int KX_port;

    Hop(byte[] ID, byte[] IPv4, byte[] IPv6){
        this.setID(ID);
        this.setIPv4(IPv4);
        this.setIPv6(IPv6);
    }

    public void setID(byte[] ID) {
        this.ID = ID;
    }

    public void setIPv4(byte[] IPv4) {
        this.IPv4 = IPv4;
    }

    public void setIPv6(byte[] IPv6) {
        this.IPv6 = IPv6;
    }

    public void setKX_port(int KX_port) {
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
}
