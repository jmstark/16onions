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

package gossip;

import java.util.Iterator;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import protocol.MessageSizeExceededException;

/**
 * Class to hold tasks that are to be periodically repeated for peer
 *
 * @author totakura
 */
class PeerContext {

    final static private Logger LOGGER = Logger.getLogger("Gossip");
    final private Peer peer;
    final private ScheduledExecutorService executor;
    final private Cache cache;
    private ScheduledFuture future_shareNeighbours;

    PeerContext(Peer peer,
            ScheduledExecutorService scheduled_executor,
            Cache cache) {
        this.peer = peer;
        this.executor = scheduled_executor;
        this.cache = cache;
    }

    Peer getPeer() {
        return peer;
    }

    protected void _shareNeighbors() {
        NeighboursMessage message;
        Iterator<Peer> iterator;
        Peer neighbor;
        message = null;
        iterator = cache.peerIterator();
        while (iterator.hasNext()) {
            neighbor = iterator.next();
            if (peer == neighbor) {
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
            LOGGER.log(Level.WARNING, "We do not know any peers to share them");
            return;
        }
        peer.sendMessage(message);
    }

    void shareNeighbours() {
        ScheduledFuture future;

        future = executor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                PeerContext.this._shareNeighbors();
            }
        }, 0, 30, TimeUnit.SECONDS);
        this.future_shareNeighbours = future;
    }
}
