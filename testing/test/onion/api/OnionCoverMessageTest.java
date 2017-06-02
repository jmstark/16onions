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
import protocol.Message;
import protocol.Protocol;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class OnionCoverMessageTest {
    private static final ByteBuffer buffer = ByteBuffer.allocate(
            Protocol.MAX_MESSAGE_SIZE * 2);
    private static int coverSize;

    static {
        Random random = new Random();
        coverSize = random.nextInt(Message.UINT16_MAX);
    }
    private final OnionCoverMessage instance;

    public OnionCoverMessageTest() {
        instance = new OnionCoverMessage(coverSize);
    }

    /**
     * Test of getCoverSize method, of class OnionCoverMessage.
     */
    @Test
    public void testGetCoverSize() {
        System.out.println("getCoverSize");
        int result = instance.getCoverSize();
        assertEquals(coverSize, result);
    }

    /**
     * Test of send method, of class OnionCoverMessage.
     */
    @Test
    public void testSend() {
        System.out.println("send");
        buffer.clear();
        instance.send(buffer);
        buffer.flip();
    }

    /**
     * Test of parse method, of class OnionCoverMessage.
     */
    @Test
    public void testParse() throws Exception {
        System.out.println("parse");
        testSend();
        buffer.getInt(); //skip 4 bytes of the header
        OnionCoverMessage result = OnionCoverMessage.parse(buffer);
        assertEquals(instance, result);
    }

}
