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

import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.util.Random;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import protocol.Message;
import protocol.MessageSizeExceededException;
import protocol.Protocol;
import tools.SecurityHelper;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class OnionAuthSessionHS1Test {

    static final ByteBuffer buffer = ByteBuffer.allocate(
            Protocol.MAX_MESSAGE_SIZE * 2);
    static final KeyPair keyPair = tools.SecurityHelper.generateRSAKeyPair(2048);
    static final long sessionID;
    static final byte[] publicKeyEnc;

    static {
        Random rand = new Random();
        sessionID = Message.unsignedLongFromInt(rand.nextInt());
        publicKeyEnc = SecurityHelper.encodeRSAPublicKey(keyPair.getPublic());
    }

    private OnionAuthSessionHS1 message;

    public OnionAuthSessionHS1Test() throws MessageSizeExceededException {
        message = new OnionAuthSessionHS1(sessionID, publicKeyEnc);
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getId method, of class OnionAuthSessionHS1.
     */
    @Test
    public void testGetId() {
        System.out.println("getId");
        long expResult = sessionID;
        long result = message.getId();
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
        OnionAuthSessionHS1 expResult = message;
        OnionAuthSessionHS1 result = OnionAuthSessionHS1.parse(buffer);
        assertEquals(expResult, result);
    }

}
