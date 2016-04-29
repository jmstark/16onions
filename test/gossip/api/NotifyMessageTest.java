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
package gossip.api;

import java.nio.ByteBuffer;
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
public class NotifyMessageTest {
    private final ByteBuffer buf;
    private final int datatype;

    public NotifyMessageTest() {
        buf = ByteBuffer.allocate(8);
        datatype = 25565;
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of send method, of class NotifyMessage.
     */
    @Test
    public void testSend() {
        buf.clear();
        NotifyMessage message;
        message = new NotifyMessage(datatype);
        message.send(buf);
        buf.flip();
        assertEquals(8, buf.remaining());
    }

    /**
     * Test of parse method, of class NotifyMessage.
     */
    @Test
    public void testParse() {
        testSend();
        buf.getShort();
        assertEquals(Message.unsignedIntFromShort(buf.getShort()),
                Protocol.MessageType.API_GOSSIP_NOTIFY.getNumVal());
        NotifyMessage result = (NotifyMessage) NotifyMessage.parse(buf);
        assertEquals(result.getDatatype(), datatype);
    }

}
