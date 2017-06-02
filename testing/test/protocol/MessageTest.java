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
package protocol;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author totakura
 */
public class MessageTest {

    public MessageTest() {
    }

    /**
     * Test of equals method, of class Message.
     */
    //@Test
    public void testEquals() {
    }

    /**
     * Test of getSize method, of class Message.
     */
    //@Test
    public void testGetSize() {
    }

    /**
     * Test of getType method, of class Message.
     */
    //@Test
    public void testGetType() {
    }

    /**
     * Test of unsignedIntFromShort method, of class Message.
     */
    @Test
    public void testUnsignedIntFromShort() {
        assertEquals(0, Message.unsignedIntFromShort((short)0));
        assertEquals((int) Short.MAX_VALUE,
                Message.unsignedIntFromShort(Short.MAX_VALUE));
        assertEquals(65535, Message.unsignedIntFromShort((short) -1));
        assertEquals(65534, Message.unsignedIntFromShort((short) -2));
        assertEquals((int) Short.MAX_VALUE+1,
                Message.unsignedIntFromShort(Short.MIN_VALUE));
    }

    /**
     * Test of unsignedLongFromInt method, of class Message.
     */
    @Test
    public void testUnsignedLongFromInt() {
        assertEquals(0L, Message.unsignedLongFromInt(0));
        assertEquals((long) Integer.MAX_VALUE,
                Message.unsignedLongFromInt(Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE + 1L,
                Message.unsignedLongFromInt(Integer.MIN_VALUE));
        assertEquals(0xffffffffL,
                Message.unsignedLongFromInt(-1));
        assertEquals(0xfffffffeL,
                Message.unsignedLongFromInt(-2));
    }

    /**
     * Test of getUnsignedShort method, of class Message.
     */
    @Test
    public void testGetUnsignedShort() {
        assertEquals((short)0, Message.unsignedShortFromByte((byte) 0));
        assertEquals((short) Byte.MAX_VALUE,
                Message.unsignedShortFromByte(Byte.MAX_VALUE));
        assertEquals((short) Byte.MAX_VALUE + 1,
                Message.unsignedShortFromByte(Byte.MIN_VALUE));
        assertEquals((short) 0xff,
                Message.unsignedShortFromByte((byte) -1));
        assertEquals((short) 0xfe,
                Message.unsignedShortFromByte((byte) -2));
    }

    public class MessageImpl extends Message {
    }

}
