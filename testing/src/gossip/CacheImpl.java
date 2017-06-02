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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
class CacheImpl extends Cache {

    private final static Logger LOGGER = Logger.getLogger("gossip.Cache");
    private final List<Peer> peers;
    //Items which are not yet validated
    private final List<Item> newItems;
    //Items which are validated
    private final List<Item> validItems;
    private final ReentrantLock lock_peers;
    private final ReentrantLock lock_dataitems;
    private final int max_peers;
    private final int max_dataitems;

    protected CacheImpl(int capacity) {
        this.peers = new LinkedList();
        this.newItems = new LinkedList();
        this.validItems = new LinkedList();
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
                LOGGER.log(Level.FINEST, "{0} already exists in cache", peer);
                return peers.get(index);
            }
            LOGGER.log(Level.FINEST, "Adding {0} to cache", peer);
            status = peers.add(peer);
            assert (status); //adding should succeed because we checked it before
            if (peers.size() > max_peers) {
                LOGGER.log(Level.FINE,
                        "Removing an older peer to accommodate new ones");
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

    /**
     * Add the given item into the cache.
     *
     * The validation is pending on the item. This means that the item is not
     * retrieved using the @a getItem() call until it is validated.
     *
     * If the given item is already present in the cache, it is not added again.
     * Instead the older object representing the same item is returned.
     *
     * @param item the item to add
     * @return null when the given item is new and is added into the cache; the
     * existing item object when the given item is already present in the cache.
     */
    @Override
    public Item addItem(Item item) {
        int index;
        lock_dataitems.lock();
        try {
            // check if the item is already among valid items
            index = validItems.indexOf(item);
            if (-1 != index) {
                LOGGER.finest("An existing item is found; not adding new one");
                return validItems.get(index);
            }
            // check if the item is already among new items
            index = newItems.indexOf(item);
            if (-1 != index) {
                LOGGER.finest("An existing item is found pending validation;"
                        + " discarding new one");
                return newItems.get(index);
            }
            // if the new items list is too big; remove older item
            if (newItems.size() == max_dataitems) {
                LOGGER.finest("Removing an older item to accommodate new one");
                newItems.remove(0);
            }
            LOGGER.finest("Adding an item to new items");
            newItems.add(item);
        } finally {
            lock_dataitems.unlock();
        }
        return null;
    }

    /**
     * Mark the given item as valid.
     *
     * An item is propagated via Gossip to other peers if it is valid.
     *
     * @param item item to be validated
     */
    @Override
    public void markValid(Item item) {
        int index;
        lock_dataitems.lock();
        try {
            // check if the item is already among valid items
            index = validItems.indexOf(item);
            if (-1 != index) {
                LOGGER.finest("Item already in valid items; not adding again");
                return;
            }
            // check if the item is in new items and move it into valid items
            index = newItems.indexOf(item);
            if (-1 == index) {
                LOGGER.fine("Item not found in new items; not adding");
                return;
            }
            newItems.remove(index);
            if (validItems.size() == max_dataitems) {
                LOGGER.finest(
                        "Removing an older valid item to accommodate new one");
                validItems.remove(0);
            }
            LOGGER.finest("Adding an item to valid items");
            validItems.add(item);
        } finally {
            lock_dataitems.unlock();
        }
    }

    /**
     * Return the list of validated items.
     *
     * @return the list of validated items
     */
    @Override
    public List<Item> getItems() {
        ArrayList<Item> items;
        lock_dataitems.lock();
        try {
            items = new ArrayList(validItems);
        } finally {
            lock_dataitems.unlock();
        }
        return items;
    }
}
