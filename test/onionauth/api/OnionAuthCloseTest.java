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
import static onionauth.api.OnionAuthEncryptTest.payload;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import protocol.Message;
import protocol.Protocol;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class OnionAuthCloseTest {
    static final ByteBuffer buffer = ByteBuffer.allocate(
            Protocol.MAX_MESSAGE_SIZE * 2);
    static final int sessionId;

    static {
        Random rand = new Random();
        sessionId = rand.nextInt(65536);
    }
    private final OnionAuthClose message;

    public OnionAuthCloseTest() {
        message = new OnionAuthClose(sessionId);
    }

    /**
     * Test of getSessionID method, of class OnionAuthClose.
     */
    @Test
    public void testGetSessionID() {
        System.out.println("getSessionID");
        long expResult = sessionId;
        long result = message.getSessionID();
        assertEquals(expResult, result);
    }

    /**
     * Test of send method, of class OnionAuthClose.
     */
    @Test
    public void testSend() {
        System.out.println("send");
        buffer.clear();
        message.send(buffer);
    }

    /**
     * Test of parse method, of class OnionAuthClose.
     */
    @Test
    public void testParse() throws Exception {
        System.out.println("parse");
        testSend();
        buffer.flip();
        buffer.position(4);
        OnionAuthClose expResult = message;
        OnionAuthClose result = OnionAuthClose.parse(buffer);
        assertEquals(expResult, result);
    }

}
