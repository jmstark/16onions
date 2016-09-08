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
package rps;

import java.net.InetSocketAddress;
import util.config.Configuration;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public interface RpsConfiguration extends Configuration {
    
    /**
     * Return the periodicity in seconds at which we have to publish our hostkey
     * in Gossip
       * 
     * @return 
     */
    public int getPublishInterval();

    /**
     * Get the socket address for the Gossip module
     *
     * @return socket address
     */
    public InetSocketAddress getGossipAPIAddress();

    /**
     * Get the socket address where the onion module on the current peer listens
     * for p2p connections
     *
     * @return the socket address
     */
    public InetSocketAddress getOnionP2PAddress();

}
