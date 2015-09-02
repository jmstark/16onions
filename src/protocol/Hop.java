/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package protocol;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 *
 * @author Emertat
 */
public class Hop {
    private String ID, IPv4, IPv6;
    private int KX_port;

    public void setID(String ID) {
        this.ID = ID;
    }

    public void setIPv4(String IPv4) {
        this.IPv4 = IPv4;
    }

    public void setIPv6(String IPv6) {
        this.IPv6 = IPv6;
    }

    public void setKX_port(int KX_port) {
        this.KX_port = KX_port;
    }

    public String getID() {
        return ID;
    }

    public String getIPv4() {
        return IPv4;
    }

    public String getIPv6() {
        return IPv6;
    }

    public int getKX_port() {
        return KX_port;
    }

    @Override
    public String toString() {
        String res = getID() + Protocol.twoBytesFormat(getKX_port());
        for(int i = 0 ; i < Protocol.DHT_TRACE_REPLY_RESERVED_BYTES; i++){
            res +="-";
        }
        res += getIPv4() + getIPv6();
        return res;
    }

    public void serialize(ByteBuffer out) {
        out.put(this.ID.getBytes(StandardCharsets.US_ASCII));
        out.putShort( (short) this.KX_port);
        out.putShort((short) 0);//reserved
        out.put(this.IPv4.getBytes(StandardCharsets.US_ASCII));
        out.put(this.IPv6.getBytes(StandardCharsets.US_ASCII));
    }

    //ID: 32; KX port + reserved: 4; IPv4: 4; IPv6: 16
    public static final int WIRE_SIZE = 32 + 4 + 4 + 16;
}