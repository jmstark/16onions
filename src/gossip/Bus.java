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

/**
 * Minimalistic dispatcher for Gossip notifications
 *
 * @author totakura
 */
public final class Bus {

    private static final Bus BUS = new Bus();
    private final Map<Integer, List<NotificationHandler>> handlerMap;

    public Bus() {
        this.handlerMap = new HashMap();
    }

    public synchronized void addHandler(NotificationHandler handler) {
        List<NotificationHandler> handlers;
        handlers = this.handlerMap.get(handler.getDatatype());
        if (null == handlers) {
            handlers = new LinkedList();
            this.handlerMap.put(handler.getDatatype(), handlers);
        }
        handlers.add(handler);
    }

    public synchronized void removeHandler(NotificationHandler handler) {
        List<NotificationHandler> handlers;
        handlers = this.handlerMap.get(handler.getDatatype());
        if (null == handlers) {
            return;
        }
        handlers.remove(handler);
    }

    public void trigger(Page page) {
        List<NotificationHandler> handlers;
        synchronized (handlerMap) {
            handlers = handlerMap.get(page.getDatatype());
            if (null == handlers) {
                return;
            }
            handlers = new ArrayList(handlers); //required due to concurrent access
        }
        for (NotificationHandler handler : handlers) {
            handler.handleData(page.getData());
        }
    }

    public static Bus getGlobal() {
        return BUS;
    }
}
