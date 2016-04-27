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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Class for local cache. The cache is used to store items(news) and information
 * about peers, both currently connected and known
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
final class Cache {
    private final List<Peer> peers;
    private final ReentrantLock lock_peers;
    private final int max_peers;
    //private final List<News> news;

    Cache(int capacity) {
        this.peers = new LinkedList();
        this.lock_peers = new ReentrantLock();
        this.max_peers = capacity;
    }

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
    Peer addPeer(Peer peer) {
        int index;
        boolean status;
        lock_peers.lock();
        try {
            index = peers.indexOf(peer);
            if (-1 != index) {
                return peers.get(index);
            }
            status = peers.add(peer);
            assert (status); //adding should succeed because we checked it before
            if (peers.size() > max_peers) {
                peers.remove(0);
            }
            return null;
        } finally {
            lock_peers.unlock();
        }
    }

    boolean removePeer(Peer peer) {
        lock_peers.lock();
        try {
            return peers.remove(peer);
        } finally {
            lock_peers.unlock();
        }
    }

    void replacePeer(Peer older, Peer newer) {
        lock_peers.lock();
        try {
            peers.remove(older);
            peers.add(newer);
        } finally {
            lock_peers.unlock();
        }
    }

    Iterator peerIterator() {
        ArrayList<Peer> list;
        lock_peers.lock(); //FIXME: Change this to ReadWriteLock
        try {
            list = new ArrayList(peers);
        } finally {
            lock_peers.unlock();
        }
        return list.iterator();
    }
}
