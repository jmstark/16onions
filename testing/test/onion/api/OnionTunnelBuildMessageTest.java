/*
 * Copyright (C) 2016 Sree Harsha Totakura <sreeharsha@totakura.in>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package onion.api;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import org.junit.Test;
import static org.junit.Assert.*;
import protocol.MessageSizeExceededException;
import protocol.Protocol;
import util.SecurityHelper;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class OnionTunnelBuildMessageTest {
    private static final ByteBuffer buffer = ByteBuffer.allocate(
            Protocol.MAX_MESSAGE_SIZE * 2);
    private static final KeyPair keyPair;
    private static final byte[] publicKeyEnc;
    private static final InetSocketAddress address;

    static {
        keyPair = util.SecurityHelper.generateRSAKeyPair(2048);
        publicKeyEnc = SecurityHelper.encodeRSAPublicKey(keyPair.getPublic());
        try {
            address = new InetSocketAddress(InetAddress.getByName("127.0.0.1"),
                    7004);
        } catch (UnknownHostException ex) {
            throw new RuntimeException();
        }
    }
    private final OnionTunnelBuildMessage message;

    public OnionTunnelBuildMessageTest() throws MessageSizeExceededException {
        message = new OnionTunnelBuildMessage(address, (RSAPublicKey) keyPair.
                getPublic());
    }

    /**
     * Test of getAddress method, of class OnionTunnelBuildMessage.
     */
    @Test
    public void testGetAddress() {
        System.out.println("getAddress");
        InetSocketAddress result = message.getAddress();
        assertEquals(address, result);
    }

    /**
     * Test of getKey method, of class OnionTunnelBuildMessage.
     */
    @Test
    public void testGetKey() {
        System.out.println("getKey");
        RSAPublicKey result = message.getKey();
        assertEquals(keyPair.getPublic(), result);
    }

    /**
     * Test of getEncoding method, of class OnionTunnelBuildMessage.
     */
    @Test
    public void testGetEncoding() {
        System.out.println("getEncoding");
        byte[] result = message.getEncoding();
        assertArrayEquals(publicKeyEnc, result);
    }

    /**
     * Test of send method, of class OnionTunnelBuildMessage.
     */
    @Test
    public void testSend() {
        System.out.println("send");
        buffer.clear();
        message.send(buffer);
        buffer.flip();
        assert (0 != buffer.remaining());
        assert (buffer.remaining() > publicKeyEnc.length);
    }

    /**
     * Test of parse method, of class OnionTunnelBuildMessage.
     */
    @Test
    public void testParse() throws Exception {
        System.out.println("parse");
        testSend();
        int pos = buffer.position();
        buffer.position(pos + 4);
        OnionTunnelBuildMessage result = OnionTunnelBuildMessage.parse(buffer);
        assertEquals(message, result);
    }

}
