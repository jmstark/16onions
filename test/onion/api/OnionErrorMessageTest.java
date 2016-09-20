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
import protocol.Protocol;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class OnionErrorMessageTest {
    private static final ByteBuffer buffer = ByteBuffer.allocate(
            Protocol.MAX_MESSAGE_SIZE * 2);
    private static final Protocol.MessageType type = Protocol.MessageType.API_ONION_TUNNEL_BUILD;
    private static long id;

    static {
        Random random = new Random();
        id = random.nextInt(Integer.MAX_VALUE);
    }
    private final OnionErrorMessage instance;

    public OnionErrorMessageTest() {
        instance = new OnionErrorMessage(type, id);
    }

    /**
     * Test of getRequestType method, of class OnionErrorMessage.
     */
    @Test
    public void testGetRequestType() {
        System.out.println("getRequestType");
        Protocol.MessageType result = instance.getRequestType();
        assertEquals(type, result);
    }

    /**
     * Test of getId method, of class OnionErrorMessage.
     */
    @Test
    public void testGetId() {
        System.out.println("getId");
        long result = instance.getId();
        assertEquals(id, result);
    }

    /**
     * Test of send method, of class OnionErrorMessage.
     */
    @Test
    public void testSend() {
        System.out.println("send");
        buffer.clear();
        instance.send(buffer);
        buffer.flip();
    }

    /**
     * Test of parser method, of class OnionErrorMessage.
     */
    @Test
    public void testParser() throws Exception {
        System.out.println("parser");
        buffer.position(buffer.position() + 4);
        OnionErrorMessage result = OnionErrorMessage.parser(buffer);
        assertEquals(instance, result);
    }

}
