/*
 * Copyright (C) 2017 Sree Harsha Totakura <sreeharsha@totakura.in>
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
package auth.api;

import auth.api.OnionAuthCipherEncrypt;
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
import util.MyRandom;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class OnionAuthCipherEncryptTest {

    static final  ByteBuffer buffer = ByteBuffer.allocate(
            Protocol.MAX_MESSAGE_SIZE * 2);
    static final byte[] payload;
    static final int sessionID;
    static final long requestID;
    static final boolean isCipher;

    static {
        sessionID = MyRandom.randInt(0, Message.UINT16_MAX);
        requestID = MyRandom.randUInt();
        isCipher = MyRandom.randInt(0, 1) == 0;
        payload = MyRandom.randBytes(MyRandom.randInt(6400,
                Protocol.MAX_MESSAGE_SIZE - 50));
    }
    private final OnionAuthCipherEncrypt message;

    public OnionAuthCipherEncryptTest() throws MessageSizeExceededException {
        message = new OnionAuthCipherEncrypt(isCipher, requestID, sessionID,
                payload);
    }


    /**
     * Test of send method, of class OnionAuthCipherEncrypt.
     */
    @Test
    public void testSend() {
        System.out.println("send");
        buffer.clear();
        message.send(buffer);
        assertTrue(buffer.remaining() > 10);
    }

    /**
     * Test of parse method, of class OnionAuthCipherEncrypt.
     */
    @Test
    public void testParse() throws Exception {
        System.out.println("parse");
        buffer.clear();
        message.send(buffer);
        buffer.flip();
        buffer.position(4);
        OnionAuthCipherEncrypt result = OnionAuthCipherEncrypt.parse(buffer);
        assertEquals(message, result);
    }

    /**
     * Test of isCipher method, of class OnionAuthCipherEncrypt.
     */
    @Test
    public void testIsCipher() {
        System.out.println("isCipher");
        boolean result = message.isCipher();
        assertEquals(isCipher, result);
    }

    /**
     * Test of getRequestID method, of class OnionAuthCipherEncrypt.
     */
    @Test
    public void testGetRequestID() {
        System.out.println("getRequestID");
        long result = message.getRequestID();
        assertEquals(requestID, result);
    }

    /**
     * Test of getSessionID method, of class OnionAuthCipherEncrypt.
     */
    @Test
    public void testGetSessionID() {
        System.out.println("getSessionID");
        int result = message.getSessionID();
        assertEquals(sessionID, result);
    }

    /**
     * Test of getPayload method, of class OnionAuthCipherEncrypt.
     */
    @Test
    public void testGetPayload() {
        System.out.println("getPayload");
        byte[] result = message.getPayload();
        assertArrayEquals(payload, result);
    }
}
