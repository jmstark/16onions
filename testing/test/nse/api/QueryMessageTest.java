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
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import protocol.Protocol;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class QueryMessageTest {

    private static ByteBuffer buffer;

    private final QueryMessage instance;

    public QueryMessageTest() {
        instance = new QueryMessage();
    }

    @BeforeClass
    public static void setUpClass() {
        buffer = ByteBuffer.allocate(Protocol.MAX_MESSAGE_SIZE);
    }

    /**
     * Test of parse method, of class QueryMessage.
     */
    @Test
    public void testParse() {
        System.out.println("parse");
        buffer.clear();
        instance.send(buffer);
        buffer.position(2);
        int type = buffer.getShort();
        assertEquals(Protocol.MessageType.API_NSE_QUERY.getNumVal(), type);
        QueryMessage result = QueryMessage.parse(buffer);
        assertEquals(instance, result);
    }

}
