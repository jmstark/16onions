/*
 * Copyright (C) 2016 totakura
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
package rps.api;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import protocol.MessageSizeExceededException;
import protocol.Protocol;

/**
 *
 * @author totakura
 */
public class RpsPeerMessageTest {

    static final ByteBuffer buffer = ByteBuffer.allocate(
            Protocol.MAX_MESSAGE_SIZE * 2);
    static final KeyPair keyPair = util.SecurityHelper.generateRSAKeyPair(2048);
    private final RpsPeerMessage message;
    private final InetSocketAddress address;

    public RpsPeerMessageTest() throws MessageSizeExceededException {
        address = new InetSocketAddress(7004);
        message = new RpsPeerMessage(address, (RSAPublicKey) keyPair.getPublic());
    }

    /**
     * Test of getAddress method, of class RpsPeerMessage.
     */
    @Test
    public void testGetAddress() {
        System.out.println("getAddress");
        assertEquals(address, message.getAddress());
    }

    /**
     * Test of getHostkey method, of class RpsPeerMessage.
     */
    @Test
    public void testGetHostkey() {
        System.out.println("getHostkey");
        assertEquals(keyPair.getPublic(), message.getHostkey());
    }

    /**
     * Test of send method, of class RpsPeerMessage.
     */
    @Test
    public void testSend() {
        System.out.println("send");
        buffer.clear();
        message.send(buffer);
        buffer.flip();
        int remaining = buffer.remaining();
        assertTrue(buffer.getShort() == remaining);
        assertTrue(buffer.getShort()
                == Protocol.MessageType.API_RPS_PEER.getNumVal());
    }

    /**
     * Test of parse method, of class RpsPeerMessage.
     */
    @Test
    public void testParse() throws Exception {
        System.out.println("parse");
        testSend();
        RpsPeerMessage result = RpsPeerMessage.parse(buffer);
        assertEquals(message, result);
    }

}
