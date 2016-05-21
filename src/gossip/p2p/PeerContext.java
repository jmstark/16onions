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

package gossip.p2p;

import gossip.Cache;
import gossip.Item;
import gossip.Peer;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import protocol.MessageSizeExceededException;

/**
 * Class to hold tasks that are to be periodically repeated for peer
 *
 * @author totakura
 */
public final class PeerContext {

    final static private Logger LOGGER = Logger.getLogger("Gossip");
    final private Peer peer;
    final private ScheduledExecutorService executor;
    final private Cache cache;
    final private Random rand;
    final private ReentrantLock lock_itemExchange;
    private ScheduledFuture future_shareNeighbours;
    private ScheduledFuture future_itemExchange;
    private boolean helloSent;

    public PeerContext(Peer peer,
            ScheduledExecutorService scheduled_executor) {
        this.peer = peer;
        this.executor = scheduled_executor;
        this.cache = Cache.getInstance();
        this.future_shareNeighbours = null;
        this.helloSent = false;
        this.rand = new Random();
        this.lock_itemExchange = new ReentrantLock();
    }

    public Peer getPeer() {
        return peer;
    }

    private void doShareNeighbors() {
        NeighboursMessage message;
        Iterator<Peer> iterator;
        Peer neighbor;
        message = null;
        iterator = cache.peerIterator();
        while (iterator.hasNext()) {
            neighbor = iterator.next();
            if (peer.equals(neighbor)) {
                continue;
            }
            try {
                if (null == message) {
                    message = new NeighboursMessage(neighbor);
                } else {
                    message.addNeighbour(neighbor);
                }
            } catch (MessageSizeExceededException ex) {
                break;
            }
        }
        if (null == message) {
            LOGGER.log(Level.WARNING,
                    "We do not know any other peers to share with {0}",
                    peer.toString());
            return;
        }
        peer.sendMessage(message);
    }

    private void rescheduleItemExchange() {
        this.lock_itemExchange.lock();
        try {
            future_itemExchange = null;
            scheduleItemExchange();
        } finally {
            this.lock_itemExchange.unlock();
        }
    }

    private void doItemExchange() {
        List<Item> items;
        DataMessage message;

        items = cache.getItems();
        if ((null == items) || (0 == items.size())) {
            LOGGER.log(Level.FINE, "There are no items to share; rescheduling");
            rescheduleItemExchange();
            return;
        }
        for (Item item : items) {
            if (item.isKnownTo(this.peer)) {
                continue;
            }
            try {
                message = DataMessage.create(item.getType(), item.getData());
            } catch (MessageSizeExceededException ex) {
                LOGGER.log(Level.SEVERE,
                        "Unexpected exception thrown; please report. Exception: {0}",
                        ex.toString());
                continue;
            }
            peer.sendMessage(message);
            item.knownTo(peer);
            break;
        }
        rescheduleItemExchange();
    }

    public void shareNeighbours() {
        if (null != future_shareNeighbours) {
            return;
        }
        future_shareNeighbours = executor.scheduleWithFixedDelay(
                new Runnable() {
            @Override
            public void run() {
                PeerContext.this.doShareNeighbors();
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    public void shutdown() {
        if (null != future_shareNeighbours) {
            future_shareNeighbours.cancel(true);
        }
        // cancel item exchange task
        this.lock_itemExchange.lock();
        try {
            if (null != future_itemExchange) {
                future_itemExchange.cancel(true);
            }
        } finally {
            this.lock_itemExchange.unlock();
        }
    }

    /**
     * Send HELLO message on the peer's connection.
     */
    public void sendHello(InetSocketAddress listen_address) {
        assert (peer.isConnected());
        assert (!helloSent); //Check we only send this once
        peer.sendMessage(HelloMessage.create(listen_address));
        helloSent = true;
    }

    public void scheduleItemExchange() {
        int delayms;

        delayms = 0;
        assert (peer.isConnected());
        this.lock_itemExchange.lock();
        try {
            if (null != future_itemExchange) {
                return;
            }
            delayms = rand.nextInt(60000) + 1;
            future_itemExchange = executor.schedule(new Runnable() {
                @Override
                public void run() {
                    PeerContext.this.doItemExchange();
                }
            }, delayms, TimeUnit.MILLISECONDS);
        } finally {
            this.lock_itemExchange.unlock();
        }
        LOGGER.log(Level.FINE, "Scheduled item exchange to run after {0} ms",
                delayms);
    }
}
