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
package gossip.api;

import gossip.p2p.Page;
import java.nio.ByteBuffer;
import java.util.Random;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import protocol.Message;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;

/**
 *
 * @author totakura
 */
public class NotificationMessageTest {

    private final ByteBuffer buf;
    private Page page;
    private NotificationMessage orig;

    public NotificationMessageTest() {
        buf = ByteBuffer.allocate(Protocol.MAX_MESSAGE_SIZE);
    }

    @Before
    public void setUp() {
        Random rand;
        byte[] data;

        rand = new Random();
        data = new byte[rand.nextInt(Protocol.MAX_MESSAGE_SIZE - 20)];
        this.page = new Page(rand.nextInt(65535), data);
    }

    /**
     * Test of send method, of class NotificationMessage.
     */
    @Test
    public void testSend() {
        buf.clear();
        try {
            orig = new NotificationMessage(this.page);
        } catch (MessageSizeExceededException e) {
            Assert.fail();
        }
        orig.send(buf);
        buf.flip();
        Assert.assertTrue(8 <= buf.remaining());
    }

    /**
     * Test parsing of the message
     * @throws protocol.MessageParserException
     */
    @Test
    public void testParse() throws MessageParserException {
        testSend();
        buf.getShort();
        Assert.assertEquals(Protocol.MessageType.API_GOSSIP_NOTIFICATION.getNumVal(),
                Message.unsignedIntFromShort(buf.getShort()));
        NotificationMessage result = (NotificationMessage) NotificationMessage.parse(buf);
        Assert.assertEquals(orig, result);
    }

}
