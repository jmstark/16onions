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

import auth.api.OnionAuthCipherDecrypt;
import auth.api.OnionAuthCipherDecryptResp;
import java.nio.ByteBuffer;
import static auth.api.OnionAuthCipherDecryptTest.requestID;
import static auth.api.OnionAuthCipherEncryptTest.isCipher;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import protocol.Message;
import protocol.MessageSizeExceededException;
import protocol.Protocol;
import util.MyRandom;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class OnionAuthCipherDecryptRespTest {
    static final ByteBuffer buffer = ByteBuffer.allocate(
            Protocol.MAX_MESSAGE_SIZE * 2);
    static final byte[] payload;
    static final long requestID;
    static final boolean isCipher;

    static {
        requestID = MyRandom.randUInt();
        isCipher = MyRandom.randInt(0, 1) == 0;
        payload = MyRandom.randBytes(MyRandom.randInt(6400,
                Protocol.MAX_MESSAGE_SIZE - 50));

    }
    private final OnionAuthCipherDecryptResp message;

    public OnionAuthCipherDecryptRespTest() throws MessageSizeExceededException {
        message = new OnionAuthCipherDecryptResp(isCipher, requestID, payload);
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
        OnionAuthCipherDecryptResp expResult = message;
        buffer.clear();
        message.send(buffer);
        buffer.flip();
        buffer.position(4);
        OnionAuthCipherDecryptResp result
                = OnionAuthCipherDecryptResp.parse(buffer);
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
    public void testRequestID() {
        System.out.println("testRequestID");
        assertEquals(message.getRequestID(), requestID);
    }

    @Test
    public void testIsCipher() {
        System.out.println("testIsCipher");
        assertEquals(message.isCipher(), isCipher);
    }
}
