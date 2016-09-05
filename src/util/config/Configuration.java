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
package util.config;

import java.net.InetSocketAddress;
import java.util.NoSuchElementException;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public interface Configuration {

    public InetSocketAddress getAddress(String option) throws
            NoSuchElementException;

        /**
     * Get the address of the socket used for listening to P2P connections from
     * other peers.
     *
     * @return the socket address
     * @throws NoSuchElementException when the configuration does not have the
     * address and no default is present.
     */
    public InetSocketAddress getListenAddress()
            throws NoSuchElementException;

    /**
     * Get the address of the socket used for listening to API connections from
     * other modules
     *
     * @return the socket address
     * @throws NoSuchElementException when the configuration does not have the
     * address and no default is present.
     */
    public InetSocketAddress getAPIAddress()
            throws NoSuchElementException;
}
