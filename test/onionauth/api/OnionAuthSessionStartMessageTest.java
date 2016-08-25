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
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.util.Random;
import static onionauth.api.OnionAuthSessionHS1Test.sessionID;
import org.junit.After;
import org.junit.AfterClass;
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
public class OnionAuthSessionStartMessageTest {
    static final ByteBuffer buffer = ByteBuffer.allocate(
            Protocol.MAX_MESSAGE_SIZE * 2);
    static final KeyPair keyPair = tools.SecurityHelper.generateRSAKeyPair(2048);
    static final byte[] publicKeyEnc;

    static {
        publicKeyEnc = SecurityHelper.encodeRSAPublicKey(keyPair.getPublic());
    }
    private OnionAuthSessionStartMessage message;

    public OnionAuthSessionStartMessageTest() throws
            MessageSizeExceededException {
        message = new OnionAuthSessionStartMessage((RSAPublicKey) keyPair.
                getPublic());
    }

    /**
     * Test of getKeyEnc method, of class OnionAuthSessionStartMessage.
     */
    @Test
    public void testGetKeyEnc() throws InvalidKeyException {
        System.out.println("getKeyEnc");
        byte[] expResult = publicKeyEnc;
        byte[] result = message.getKeyEnc();
        assertArrayEquals(expResult, result);
        RSAPublicKey pkey = SecurityHelper.getRSAPublicKeyFromEncoding(result);
    }

    /**
     * Test of getPKey method, of class OnionAuthSessionStartMessage.
     */
    @Test
    public void testGetPKey() {
        System.out.println("getPKey");
        RSAPublicKey expResult = (RSAPublicKey) keyPair.getPublic();
        RSAPublicKey result = message.getPKey();
        assertEquals(expResult, result);
    }

    /**
     * Test of send method, of class OnionAuthSessionStartMessage.
     */
    @Test
    public void testSend() {
        System.out.println("send");
        buffer.clear();
        message.send(buffer);
    }

    /**
     * Test of parse method, of class OnionAuthSessionStartMessage.
     */
    @Test
    public void testParse() throws Exception {
        System.out.println("parse");
        testSend();
        buffer.flip();
        buffer.position(4);
        OnionAuthSessionStartMessage result = OnionAuthSessionStartMessage.
                parse(buffer);
        assertEquals(message, result);
    }

}
