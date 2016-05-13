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
package gossip.p2p;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class Page {
    private final int datatype;
    private final byte[] data;

    public Page(int datatype, byte[] data) {
        this.datatype = datatype;
        this.data = data;
    }

    public int getDatatype() {
        return datatype;
    }

    public byte[] getData() {
        return data;
    }
}
