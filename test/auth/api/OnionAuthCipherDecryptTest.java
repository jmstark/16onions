/*
 * Copyright (C) 2017 totakura
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

import auth.api.OnionAuthCipherDecrypt;
import java.nio.ByteBuffer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import protocol.Message;
import protocol.MessageSizeExceededException;
import protocol.Protocol;
import util.MyRandom;

/**
 *
 * @author totakura
 */
public class OnionAuthCipherDecryptTest {

    static final ByteBuffer buffer = ByteBuffer.allocate(
            Protocol.MAX_MESSAGE_SIZE * 2);
    static final byte[] payload;
    static final int sessionID;
    static final long requestID;

    static {
        sessionID = MyRandom.randInt(0, Message.UINT16_MAX);
        requestID = MyRandom.randUInt();
        payload = MyRandom.randBytes(MyRandom.randInt(6400,
                Protocol.MAX_MESSAGE_SIZE - 50));
    }
    private final OnionAuthCipherDecrypt message;

    public OnionAuthCipherDecryptTest() throws MessageSizeExceededException {
        message = new OnionAuthCipherDecrypt(requestID, sessionID, payload);
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
     * Test of parse method, of class OnionAuthCipherDecrypt.
     */
    @Test
    public void testParse() throws Exception {
        System.out.println("parse");
        OnionAuthCipherDecrypt expResult = message;
        buffer.clear();
        message.send(buffer);
        buffer.flip();
        buffer.position(4);
        OnionAuthCipherDecrypt result = OnionAuthCipherDecrypt.parse(buffer);
        assertEquals(expResult, result);
    }

    /**
     * Test of equals method, of class OnionAuthCipherDecrypt.
     */
    @Test
    public void testPayload() {
        System.out.println("testPayload");
        assertEquals(message.getPayload(), payload);
    }

    @Test
    public void testSessionID() {
        System.out.println("testSessionID");
        assertEquals(message.getSessionID(), sessionID);
    }

    @Test
    public void testRequestID() {
        System.out.println("testRequestID");
        assertEquals(message.getRequestID(), requestID);
    }

}
