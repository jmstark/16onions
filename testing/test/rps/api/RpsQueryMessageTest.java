/*
 * Copyright (C) 2016 totakura
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
package rps.api;

import java.nio.ByteBuffer;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import protocol.Protocol;

/**
 *
 * @author totakura
 */
public class RpsQueryMessageTest {
    static final ByteBuffer buffer = ByteBuffer.allocate(
            Protocol.MAX_MESSAGE_SIZE * 2);
    private final RpsQueryMessage message;

    public RpsQueryMessageTest() {
        message = new RpsQueryMessage();
    }

    /**
     * Test of send method, of class RpsQueryMessage.
     */
    @Test
    public void testSend() {
        System.out.println("send");
        buffer.clear();
        message.send(buffer);
        buffer.flip();
        int remaining = buffer.remaining();
        assertEquals(buffer.getShort(), remaining);
        assertEquals(buffer.getShort(),
                Protocol.MessageType.API_RPS_QUERY.getNumVal());
    }

}
