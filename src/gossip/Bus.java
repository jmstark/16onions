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

import gossip.p2p.Page;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton minimalistic dispatcher for Gossip notifications
 *
 * @author totakura
 */
public final class Bus {

    private static final Bus BUS = new Bus();
    private final Map<Integer, List<NotificationHandler>> handlerMap;
    private static final Logger LOGGER = Logger.getLogger("Gossip");

    private Bus() {
        this.handlerMap = new HashMap();
    }

    /**
     * Adds a handler for a datatype.
     *
     * A datatype can have multiple handlers. In that case all of the handlers
     * will be called sequentially when the matching data is available
     *
     * @param datatype
     * @param handler
     */
    public synchronized void addHandler(int datatype,
            NotificationHandler handler) {
        List<NotificationHandler> handlers;
        handlers = this.handlerMap.get(datatype);
        // create a new list if none exists
        if (null == handlers) {
            handlers = new LinkedList();
            this.handlerMap.put(datatype, handlers);
        }
        handlers.add(handler);
        LOGGER.
                log(Level.FINER, "Added a notification handler for {0}",
                        datatype);
    }

    /**
     * Remove a handler from being triggered upon a datatype
     *
     * @param datatype
     * @param handler
     */
    public synchronized void removeHandler(int datatype,
            NotificationHandler handler) {
        List<NotificationHandler> handlers;
        handlers = this.handlerMap.get(datatype);
        if (null == handlers) {
            return;
        }
        handlers.remove(handler);
        LOGGER.log(Level.FINER, "Removed a notification handler");
    }

    /**
     * Trigger handlers which are interested in the given page
     *
     * @param page
     */
    public void trigger(Item item) {
        List<NotificationHandler> handlers;
        // synchronize on the hashmap
        synchronized (handlerMap) {
            handlers = handlerMap.get(item.getType());
            if (null == handlers) {
                return;
            }
            handlers = new ArrayList(handlers); //required due to concurrent access
        }
        for (NotificationHandler handler : handlers) {
            LOGGER.log(Level.FINEST,
                    "Triggering notification handler {0}", handler.toString());
            handler.handleData(item);
        }
    }

    /**
     * Function to get the singleton
     *
     * @return the singleton object of this class
     */
    public static Bus getInstance() {
        return BUS;
    }
}
