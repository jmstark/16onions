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
import java.util.Random;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import protocol.Message;
import protocol.MessageSizeExceededException;
import protocol.Protocol;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class OnionAuthEncryptTest {
    static final ByteBuffer buffer = ByteBuffer.allocate(
            Protocol.MAX_MESSAGE_SIZE * 2);
    static final byte[] payload;
    static final long[] sessions;
    static final int requestId;

    static {
        Random rand = new Random();
        payload = new byte[rand.nextInt(32000)];
        rand.nextBytes(payload);
        sessions = new long[rand.nextInt(100)];
        for (int index = 0; index < sessions.length; index++) {
            sessions[index] = Message.unsignedLongFromInt(rand.nextInt());
        }
        requestId = rand.nextInt((Short.MAX_VALUE * 2) + 2);
    }
    private final OnionAuthEncrypt message;

    public OnionAuthEncryptTest() throws MessageSizeExceededException {
        message = new OnionAuthEncrypt(requestId, sessions, payload);
    }

    /**
     * Test of getId method, of class OnionAuthEncrypt.
     */
    @Test
    public void testGetId() {
        System.out.println("getId");
        int expResult = requestId;
        int result = message.getId();
        assertEquals(expResult, result);
    }

    /**
     * Test of getSessions method, of class OnionAuthEncrypt.
     */
    @Test
    public void testGetSessions() {
        System.out.println("getSessions");
        long[] expResult = sessions;
        long[] result = message.getSessions();
        assertArrayEquals(expResult, result);
    }

    /**
     * Test of getPayload method, of class OnionAuthEncrypt.
     */
    @Test
    public void testGetPayload() {
        System.out.println("getPayload");
        byte[] expResult = payload;
        byte[] result = message.getPayload();
        assertArrayEquals(expResult, result);
    }

    /**
     * Test of send method, of class OnionAuthEncrypt.
     */
    @Test
    public void testSend() {
        System.out.println("send");
        buffer.clear();
        message.send(buffer);
    }

    /**
     * Test of parse method, of class OnionAuthEncrypt.
     */
    @Test
    public void testParse() throws Exception {
        System.out.println("parse");
        testSend();
        buffer.flip();
        buffer.position(4);
        OnionAuthEncrypt expResult = message;
        OnionAuthEncrypt result = OnionAuthEncrypt.parse(buffer);
        assertEquals(expResult, result);
    }

}
