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

import java.util.Iterator;
import java.util.List;

/**
 * Singleton class for local cache. The cache is used to store items(news) and
 * information about peers, both currently connected and known
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public abstract class Cache {

    private static Cache CACHE;

    /**
     * Add the given peer into the peer cache.
     *
     * If the peer is already present in the cache, it is not added again. If a
     * new peer is added when the cache is full, the older peer will be removed.
     *
     * @param peer the peer to add to the cache
     * @return null when the peer is added to the cache; the existing peer
     *         object when the given peer is already present in the cache
     */
    public abstract Peer addPeer(Peer peer);

    public abstract boolean removePeer(Peer peer);

    public abstract void replacePeer(Peer older, Peer newer);

    public abstract Iterator peerIterator();

    /**
     * Add the given item into the cache.
     *
     * The validation is pending on the item. This means that the item is not
     * retrived using the @a getItem() call until it is validated.
     *
     * If the given item is already present in the cache, it is not added again.
     * Instead the older object representing the same item is returned.
     *
     * @param item the item to add
     * @return null when the given item is new and is added into the cache; the
     * existing item object when the given item is already present in the cache.
     */
    public abstract Item addItem(Item item);

    /**
     * Return the list of validated items.
     *
     * @return the list of validated items
     */
    public abstract List<Item> getItems();

    /**
     * Mark the given item as valid.
     *
     * An item is propagated via Gossip to other peers if it is valid.
     *
     * @param item item to be validated
     */
    public abstract void markValid(Item item);

    public static Cache initialize(int capacity) {
        if (null != CACHE) {
            return CACHE;
        }
        CACHE = new CacheImpl(capacity);
        return CACHE;
    }

    public static Cache getInstance() {
        return CACHE;
    }
}
