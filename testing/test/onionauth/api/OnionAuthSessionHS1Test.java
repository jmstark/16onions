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
package onionauth.api;

import auth.api.OnionAuthSessionHS1;
import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;
import protocol.Message;
import protocol.MessageSizeExceededException;
import protocol.Protocol;
import util.SecurityHelper;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class OnionAuthSessionHS1Test {

    static final ByteBuffer buffer = ByteBuffer.allocate(
            Protocol.MAX_MESSAGE_SIZE * 2);
    static final KeyPair keyPair = util.SecurityHelper.generateRSAKeyPair(2048);
    static final int sessionID;
    static final long requestID;
    static final byte[] publicKeyEnc;

    static {
        Random rand = new Random();

        sessionID = rand.nextInt(Message.UINT16_MAX + 1);
        requestID = util.MyRandom.randUInt();
        publicKeyEnc = SecurityHelper.encodeRSAPublicKey(keyPair.getPublic());
    }

    private final OnionAuthSessionHS1 message;

    public OnionAuthSessionHS1Test() throws MessageSizeExceededException {
        message = new OnionAuthSessionHS1(sessionID, requestID, publicKeyEnc);
    }

    /**
     * Test of getRequestID method, of class OnionAuthSessionHS1.
     */
    @Test
    public void testGetSessionID() {
        System.out.println("getSessionID");
        int expResult = sessionID;
        int result = message.getSessionID();
        assertEquals(expResult, result);
    }

    /**
     * Test of getRequestID method, of class OnionAuthSessionHS1.
     */
    @Test
    public void testGetRequestID() {
        System.out.println("getRequestID");
        long expResult = requestID;
        long result = message.getRequestID();
        assertEquals(expResult, result);
    }

    /**
     * Test of getPayload method, of class OnionAuthSessionHS1.
     */
    @Test
    public void testGetPayload() {
        System.out.println("getPayload");
        byte[] expResult = SecurityHelper.
                encodeRSAPublicKey(keyPair.getPublic());
        byte[] result = message.getPayload();
        assertArrayEquals(expResult, result);
    }

    /**
     * Test of send method, of class OnionAuthSessionHS1.
     */
    @Test
    public void testSend() {
        System.out.println("send");
        ByteBuffer out = buffer;
        out.clear();
        message.send(out);
        assertTrue(buffer.remaining() < buffer.capacity());
    }

    /**
     * Test of parse method, of class OnionAuthSessionHS1.
     */
    @Test
    public void testParse() throws Exception {
        System.out.println("parse");
        testSend();
        buffer.flip();
        buffer.position(4);
        OnionAuthSessionHS1 expResult = message;
        OnionAuthSessionHS1 result = OnionAuthSessionHS1.parse(buffer);
        assertEquals(expResult, result);
    }

}
