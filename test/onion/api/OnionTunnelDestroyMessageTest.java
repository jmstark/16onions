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
package onion.api;

import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.util.Random;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import protocol.Protocol;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class OnionTunnelDestroyMessageTest {

    private static final ByteBuffer buffer = ByteBuffer.allocate(
            Protocol.MAX_MESSAGE_SIZE * 2);
    private static final long id;

    static {
        Random random = new Random();
        id = random.nextInt(Integer.MAX_VALUE);
    }
    private final OnionTunnelDestroyMessage message;

    public OnionTunnelDestroyMessageTest() {
        message = new OnionTunnelDestroyMessage(id);
    }

    /**
     * Test of getId method, of class OnionTunnelDestroyMessage.
     */
    @Test
    public void testGetId() {
        System.out.println("getId");
        long result = message.getId();
        assertEquals(id, result);
    }

    /**
     * Test of send method, of class OnionTunnelDestroyMessage.
     */
    @Test
    public void testSend() {
        System.out.println("send");
        buffer.clear();
        message.send(buffer);
        buffer.flip();
    }

    /**
     * Test of parser method, of class OnionTunnelDestroyMessage.
     */
    @Test
    public void testParser() throws Exception {
        System.out.println("parser");
        testSend();
        buffer.position(buffer.position() + 4);
        OnionTunnelDestroyMessage result = OnionTunnelDestroyMessage.parser(buffer);
        assertEquals(message, result);
    }

}
