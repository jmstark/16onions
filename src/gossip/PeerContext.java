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

import java.net.InetSocketAddress;
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
    private boolean helloSent;

    PeerContext(Peer peer,
            ScheduledExecutorService scheduled_executor,
            Cache cache) {
        this.peer = peer;
        this.executor = scheduled_executor;
        this.cache = cache;
        this.future_shareNeighbours = null;
        this.helloSent = false;
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

    void shareNeighbours() {
        if (null != future_shareNeighbours) {
            return;
        }
        future_shareNeighbours = executor.scheduleWithFixedDelay(
                new Runnable() {
            @Override
            public void run() {
                PeerContext.this._shareNeighbors();
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    void shutdown() {
        if (null != future_shareNeighbours) {
            future_shareNeighbours.cancel(true);
        }
    }

    /**
     * Send HELLO message on the peer's connection.
     */
    void sendHello(InetSocketAddress listen_address) {
        assert (peer.isConnected());
        assert (!helloSent); //Check we only send this once
        peer.sendMessage(HelloMessage.create(listen_address));
        helloSent = true;
    }
}
