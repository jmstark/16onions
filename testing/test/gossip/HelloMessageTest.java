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
package gossip;

import gossip.p2p.HelloMessage;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import protocol.Message;
import protocol.MessageParserException;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class HelloMessageTest {

    public HelloMessageTest() {
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
     * Test of create method, of class HelloMessage.
     * @throws protocol.MessageParserException
     */
    @Test
    public void testCreate() throws MessageParserException {
        System.out.println("create");
        InetSocketAddress sock_address = new InetSocketAddress("localhost", 6001);
        HelloMessage result = HelloMessage.create(sock_address);
        assertNotNull(result);

        ByteBuffer out = ByteBuffer.allocate(2048);
        result.send(out);
        assertFalse(out.remaining() == 2048);
        out.flip();
        out.position(4); //remove the header
        HelloMessage parsed = HelloMessage.parse(out);
        assertTrue(parsed.getPeers().size() > 0);
    }

}
