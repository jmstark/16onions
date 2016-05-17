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

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public interface Item {

    /**
     * Is the given peer aware of this item?
     *
     * We believe the given peer is aware of this item if we learnt this item
     * from that peer, or if we had earlier sent this data item to that     * peer
     *
     * @param peer
     * @return true if we belive that the given peer knows this item; false     *         if not.
     */
    public boolean isKnownTo(Peer peer);

    /**
     * Mark that the given peer is aware of this item
     *
     * @param peer
     */
    public void knownTo(Peer peer);

    /**
     * Returns the datatype of this item
     *
     * @return the datatype
     */
    public int getType();

    /**
     * returns the data of this item
     *
     * @return the data associated with this item
     */
    public byte[] getData();
}
