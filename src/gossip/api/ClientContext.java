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
package gossip.api;

import gossip.Bus;
import gossip.Item;
import gossip.NotificationHandler;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import protocol.Connection;
import protocol.MessageSizeExceededException;

/**
 *
 * @author totakura
 */
public class ClientContext implements NotificationHandler {

    private final Connection connection;
    private final List<Integer> interests;
    private final Map<Integer, Item> itemsMap;
    private final List<Item> itemsList;
    private static final int MAX_ITEMS = 300;
    private static final Logger LOGGER = Logger.getLogger("API");
    private static final Bus BUS = Bus.getInstance();

    ClientContext(Connection connection) {
        this.connection = connection;
        this.interests = new LinkedList();
        this.itemsMap = new HashMap(MAX_ITEMS);
        this.itemsList = new LinkedList();
    }

    private void addItem(Item item) {
        if (itemsList.size() == MAX_ITEMS) {
            // remove an older item if there are many items
            removeItem(itemsList.get(0));
        }
        itemsList.add(item);
        itemsMap.put(item.getId(), item);
    }

    private void removeItem(Item item) {
        assert (true == itemsList.remove(item));
        item = itemsMap.remove(item.getId());
        assert (null != item);
    }

    Item findItem(int id) {
        Item item;

        item = itemsMap.get(id);
        if (null == item) {
            return null;
        }
        removeItem(item);
        return item;
    }

    /**
     * Indicate an interest to the BUS.
     *
     * Add ourselves as a notification handler in the BUS for the given
     * datatype.
     *
     * @param datatype the datatype we are interested in
     */
    void addInterest(int datatype) {
        this.interests.add(datatype);
        BUS.addHandler(datatype, this);
    }

    public List<Integer> getInterests() {
        return interests;
    }

    /**
     * Shutdown all tasks and prepare for touchdown
     */
    void close() {
        for (Integer interest : interests) {
            BUS.removeHandler(interest, this);
        }
    }

    /**
     * We received a page; send it to the API connection
     *
     * @param item the item to send via API connection
     */
    @Override
    public void handleData(Item item) {
        NotificationMessage notification;

        try {
            notification
                    = new NotificationMessage(item);
        } catch (MessageSizeExceededException ex) {
            LOGGER.log(Level.SEVERE, "This is a bug; please report."
                    + "  Size exceeded while creating a NotificationMessage");
            return;
        }
        addItem(item);
        connection.sendMsg(notification);
    }

}
