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
import protocol.Protocol;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import protocol.Message;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class AnnounceMessageTest {

    private final short ttl;
    private final int datatype;
    private final byte[] data;
    private final Random random;
    private final ByteBuffer out;

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    public AnnounceMessageTest() {
        out = ByteBuffer.allocate(Protocol.MAX_MESSAGE_SIZE);
        random = new Random();
        ttl = (short) random.nextInt(255);
        datatype = random.nextInt(65535);
        data = new byte[random.nextInt(65500)]; //should be okay to hold the buffer
        random.nextBytes(data);
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
     * Check if instantiating an object throws size exceeded exception if given
     * too large buffer
     */
    @Test
    public void testCons() throws MessageSizeExceededException {
        thrown.expect(MessageSizeExceededException.class);
        AnnounceMessage instance = null;
        //sending with more data should create size exceeded exception
        byte[] data = new byte[Protocol.MAX_MESSAGE_SIZE];
        this.random.nextBytes(data);
        instance = new AnnounceMessage(this.ttl, this.datatype, data);
    }

    /**
     * Test of send method, of class AnnounceMessage.
     */
    @Test
    public void testSend() {
        out.clear();
        AnnounceMessage instance = null;
        try {
            instance = new AnnounceMessage(this.ttl, this.datatype, this.data);
        } catch (MessageSizeExceededException ex) {
            fail(ex.toString() + " should not be thrown");
        }
        assertNotNull(instance);
        instance.send(out);
    }

    /**
     * Test of parse method, of class AnnounceMessage.
     */
    @Test
    public void testParse() {
        testSend();
        out.flip();
        AnnounceMessage result = null;
        out.position(out.position() + 2);//skip size header
        assertEquals(Protocol.MessageType.API_GOSSIP_ANNOUNCE.getNumVal(),
                Message.unsignedIntFromShort(out.getShort()));
        try {
            result = AnnounceMessage.parse(out);
        } catch (MessageParserException ex) {
            fail("parsing failed on a valid serialized buffer");
        }
        assertNotNull(result);
        assertEquals(this.ttl, result.getTtl());
        assertEquals(this.datatype, result.getDatatype());
        assertArrayEquals(this.data, result.getData());
    }

}
