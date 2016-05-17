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
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
class CacheImpl extends Cache {
    private final List<Peer> peers;
    private final List<Item> dataitems;
    private final ReentrantLock lock_peers;
    private final ReentrantLock lock_dataitems;
    private final int max_peers;
    private final int max_dataitems;

    protected CacheImpl(int capacity) {
        this.peers = new LinkedList();
        this.dataitems = new LinkedList();
        this.lock_peers = new ReentrantLock();
        this.lock_dataitems = new ReentrantLock();
        this.max_peers = capacity;
        this.max_dataitems = 5 * capacity;
    }

    @Override
    public Peer addPeer(Peer peer) {
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

    @Override
    public boolean removePeer(Peer peer) {
        lock_peers.lock();
        try {
            return peers.remove(peer);
        } finally {
            lock_peers.unlock();
        }
    }

    @Override
    public void replacePeer(Peer older, Peer newer) {
        lock_peers.lock();
        try {
            peers.remove(older);
            peers.add(newer);
        } finally {
            lock_peers.unlock();
        }
    }

    @Override
    public Iterator peerIterator() {
        ArrayList<Peer> list;
        lock_peers.lock(); //FIXME: Change this to ReadWriteLock
        try {
            list = new ArrayList(peers);
        } finally {
            lock_peers.unlock();
        }
        return list.iterator();
    }

    @Override
    public void addItem(Item item) {
        lock_dataitems.lock();
        try {
            if (max_dataitems == dataitems.size()) {
                dataitems.remove(0);
            }
            dataitems.add(item);
        } finally {
            lock_dataitems.unlock();
        }
    }

    @Override
    public List<Item> getItems() {
        ArrayList<Item> items;
        lock_dataitems.lock();
        try {
            items = new ArrayList(dataitems);
        } finally {
            lock_dataitems.unlock();
        }
        return items;
    }
}
