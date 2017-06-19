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

import auth.api.OnionAuthSessionIncomingHS1;
import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
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
import util.SecurityHelper;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class OnionAuthSessionIncomingHS1Test {

    static final ByteBuffer buffer = ByteBuffer.allocate(
            Protocol.MAX_MESSAGE_SIZE * 2);
    static final KeyPair keyPair = util.SecurityHelper.generateRSAKeyPair(2048);
    static final long requestID;
    static final byte[] payload;

    static {
        Random rand = new Random();
        requestID = util.MyRandom.randUInt();
        payload = new byte[rand.nextInt(32000)];
        rand.nextBytes(payload);
    }
    private OnionAuthSessionIncomingHS1 message;

    public OnionAuthSessionIncomingHS1Test() throws MessageSizeExceededException {
        message = new OnionAuthSessionIncomingHS1(requestID,
                (RSAPublicKey) keyPair.getPublic(), payload);
    }

    /**
     * Test of getSourceKey method, of class OnionAuthSessionIncomingHS1.
     */
    @Test
    public void testGetSourceKey() {
        System.out.println("getSourceKey");
        RSAPublicKey expResult = (RSAPublicKey) keyPair.getPublic();
        RSAPublicKey result = message.getSourceKey();
        assertEquals(expResult, result);
    }

    /**
     * Test of getPayload method, of class OnionAuthSessionIncomingHS1.
     */
    @Test
    public void testGetPayload() {
        System.out.println("getPayload");
        byte[] expResult = payload;
        byte[] result = message.getPayload();
        assertArrayEquals(expResult, result);
    }

    /**
     * Test of send method, of class OnionAuthSessionIncomingHS1.
     */
    @Test
    public void testSend() {
        System.out.println("send");
        buffer.clear();
        message.send(buffer);
    }

    /**
     * Test of parse method, of class OnionAuthSessionIncomingHS1.
     */
    @Test
    public void testParse() throws Exception {
        System.out.println("parse");
        testSend();
        buffer.flip();
        buffer.position(4);
        OnionAuthSessionIncomingHS1 result = OnionAuthSessionIncomingHS1.parse(
                buffer);
        assertEquals(message, result);
    }
}
