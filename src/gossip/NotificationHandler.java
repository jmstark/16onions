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

/**
 *
 * @author totakura
 */
public interface NotificationHandler {

    /**
     * Returns the datatype this notification handler is interested in
     *
     * @return the datatype
     */
    public abstract int getDatatype();

    /**
     * Handle the data.
     *
     * This method is called by the dispatcher when data with interested
     * datatype is available
     *
     * @param data the data corresponding to our interest
     */
    public abstract void handleData(byte[] data);
}
