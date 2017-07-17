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
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;
import protocol.MessageSizeExceededException;
import protocol.Protocol;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class OnionTunnelDataTest {

    private static final ByteBuffer buffer = ByteBuffer.allocate(
            Protocol.MAX_MESSAGE_SIZE * 2);
    private static byte[] data;
    private static long id;

    static {
        Random random = new Random();
        id = random.nextInt(Integer.MAX_VALUE);
        data = new byte[random.nextInt(Protocol.MAX_MESSAGE_SIZE - 8)];
        random.nextBytes(data);
    }
    private final OnionTunnelDataMessage message;

    public OnionTunnelDataTest() throws MessageSizeExceededException {
        message = new OnionTunnelDataMessage(id, data);
    }

    /**
     * Test of getId method, of class OnionTunnelDataMessage.
     */
    @Test
    public void testGetId() {
        System.out.println("getId");
        long result = message.getId();
        assertEquals(id, result);
    }

    /**
     * Test of getData method, of class OnionTunnelDataMessage.
     */
    @Test
    public void testGetData() {
        System.out.println("getData");
        byte[] result = message.getData();
        assertArrayEquals(data, result);
    }

    /**
     * Test of send method, of class OnionTunnelDataMessage.
     */
    @Test
    public void testSend() {
        System.out.println("send");
        buffer.clear();
        message.send(buffer);
        buffer.flip();
    }

    /**
     * Test of parse method, of class OnionTunnelDataMessage.
     */
    @Test
    public void testParse() throws Exception {
        System.out.println("parse");
        testSend();
        buffer.position(buffer.position() + 4);
        OnionTunnelDataMessage result = OnionTunnelDataMessage.parse(buffer);
        assertEquals(message, result);
    }

}
