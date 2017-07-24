/*
 * Copyright (C) 2017 Sree Harsha Totakura <sreeharsha@totakura.in>
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
package mockups.auth;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class TestByteBuffer {
    public static void main(String[] args) {
        int size = 4 * 1024;
        ByteBuffer buf = ByteBuffer.allocate(4 * 1024);
        buf.putShort((short) 108);
        buf.flip();
        byte[] data = buf.array();
        if (data.length == size);
        assert (data.length == ((Arrays.copyOf(data, 20)).length
                + (Arrays.copyOfRange(data, 20, data.length).length)));
        System.out.println("Value: " + (byte) 255);
    }
}
