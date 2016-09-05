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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.NoSuchElementException;
import util.config.ConfigurationImpl;

public class GossipConfigurationImpl
        extends ConfigurationImpl implements GossipConfiguration {

    private static final String OPTION_CACHE_SIZE = "cache_size";
    private static final String OPTION_MAX_CONNECTIONS = "max_connections";
    private static final String OPTION_BOOTSTRAPPER = "bootstrapper";

    private static HashMap<String, String> getDefaults() {
        HashMap<String, String> map = new HashMap(5);
        map.put(OPTION_CACHE_SIZE, "60");
        map.put(OPTION_MAX_CONNECTIONS, "20");
        map.put(OPTION_BOOTSTRAPPER, "131.159.20.52:4433");
        map.put(OPTION_LISTEN_ADDRESS, "127.0.0.1:4433");
        map.put(OPTION_API_ADDRESS, "127.0.0.1:7001");
        return map;
    }

    public GossipConfigurationImpl(String filename) throws IOException {
        super(filename, "gossip", getDefaults());
    }

    @Override
    public Peer getBootstrapper() throws NoSuchElementException {
        InetSocketAddress address;
        address = this.getAddress(OPTION_BOOTSTRAPPER);
        // return null if we are the bootstrap peer
        if (address.equals(this.getListenAddress())) {
            return null;
        }
        return new Peer(address);
    }

    @Override
    public int getCacheSize() throws NoSuchElementException {
        return this.parser.get(section, OPTION_CACHE_SIZE, Integer.class);
    }

    @Override
    public int getMaxConnections() throws NoSuchElementException {
        return this.parser.get(section, OPTION_MAX_CONNECTIONS, Integer.class);
    }
}
