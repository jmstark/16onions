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
package nse.api;

import java.nio.ByteBuffer;
import java.util.Random;
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
public class EstimateMessageTest {

    private final EstimateMessage instance;
    private final int estimate;
    private final int deviation;
    private final ByteBuffer buffer;
    private static Random random;

    public EstimateMessageTest() {
        estimate = random.nextInt(Integer.MAX_VALUE);
        deviation = random.nextInt(Integer.MAX_VALUE);
        instance = new EstimateMessage(estimate, deviation);
        buffer = ByteBuffer.allocate(Protocol.MAX_MESSAGE_SIZE);
    }

    @BeforeClass
    public static void setUpClass() {
        random = new Random();
    }

    /**
     * Test of getEstimate method, of class EstimateMessage.
     */
    @Test
    public void testGetEstimate() {
        System.out.println("getEstimate");
        int result = instance.getEstimate();
        assertEquals(estimate, result);
    }

    /**
     * Test of getDeviation method, of class EstimateMessage.
     */
    @Test
    public void testGetDeviation() {
        System.out.println("getDeviation");
        int result = instance.getDeviation();
        assertEquals(deviation, result);
    }

    /**
     * Test of send method, of class EstimateMessage.
     */
    @Test
    public void testSend() {
        System.out.println("send");
        buffer.clear();
        instance.send(buffer);
    }

    /**
     * Test of parse method, of class EstimateMessage.
     */
    @Test
    public void testParse() throws Exception {
        System.out.println("parse");
        testSend();
        buffer.flip();
        buffer.position(2);
        short type = buffer.getShort();
        assertEquals(Protocol.MessageType.API_NSE_ESTIMATE.getNumVal(), type);
        EstimateMessage result = EstimateMessage.parse(buffer);
        assertEquals(0, buffer.remaining());
        assertEquals(instance, result);
    }

}
