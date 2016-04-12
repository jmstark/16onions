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

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import protocol.MessageSizeExceededException;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class NeighboursMessageTest {

    public NeighboursMessageTest() {
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
     * Test of send method, of class NeighboursMessage.
     * @throws protocol.MessageSizeExceededException
     */
    @Test
    public void testSend() throws MessageSizeExceededException {
        System.out.println("send");
        ByteBuffer out = ByteBuffer.allocate(2048);
        Peer peer = new Peer(new InetSocketAddress("localhost", 6001));
        NeighboursMessage instance = new NeighboursMessage(peer);
        instance.send(out);
        assertFalse(out.remaining() == 2048);
    }

    /**
     * Test of parse method, of class NeighboursMessage.
     * @throws java.lang.Exception
     */
    @Test
    public void testParse() throws Exception {
        ByteBuffer out = ByteBuffer.allocate(2048);
        Peer peer = new Peer(new InetSocketAddress("localhost", 6001));
        NeighboursMessage instance = new NeighboursMessage(peer);
        instance.send(out);
        assertFalse(out.remaining() == 2048);

        System.out.println("parse");
        ByteBuffer buf = (ByteBuffer) out.flip();
        buf.position(4); //remove the header
        NeighboursMessage result = NeighboursMessage.parse(buf);
        assertNotNull(result.peers);
        assertTrue(result.peers.size() > 0);
        assertNotNull(result.peers.getFirst().getAddress());
    }

    /**
     * Test message parsing with a message containing multiple peers in it
     * @throws Exception
     */
    @Test
    public void testMultipleNeighbors() throws Exception {
        ByteBuffer out = ByteBuffer.allocate(2048);
        Peer peer = new Peer(new InetSocketAddress("localhost", 6001));
        NeighboursMessage instance = new NeighboursMessage(peer);
        peer = new Peer(new InetSocketAddress("192.168.1.1", 6001));
        instance.addNeighbour(peer);
        instance.send(out);
        assertFalse(out.remaining() == 2048);

        ByteBuffer buf = (ByteBuffer) out.flip();
        buf.position(4); //remove the header
        NeighboursMessage result = NeighboursMessage.parse(buf);
        assertNotNull(result.peers);
        assertTrue(2 == result.peers.size());
        assertNotNull(result.peers.getFirst().getAddress());
        assertNotNull(result.peers.getLast().getAddress());
    }

}
