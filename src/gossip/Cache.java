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

import gossip.p2p.Page;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

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

    public abstract void addItem(Item item);

    public abstract List<Item> getItems();

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
