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

    boolean addPeer(Peer peer) {
        boolean status;
        lock_peers.lock();
        try {
            status = peers.add(peer);
            if (status && (peers.size() > max_peers)) {
                peers.remove(0);
            }
        } finally {
            lock_peers.unlock();
        }
        return status;
    }

    boolean removePeer(Peer peer) {
        lock_peers.lock();
        try {
            return peers.remove(peer);
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
