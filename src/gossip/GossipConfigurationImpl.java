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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ini4j.ConfigParser;
import org.ini4j.ConfigParser.ConfigParserException;
import tools.config.ConfigurationImpl;

public class GossipConfigurationImpl
        extends ConfigurationImpl implements GossipConfiguration {

    private static final String optionCacheSize = "cache_size";
    private static final String optionMaxConnections = "max_connections";
    private static final String optionBootstrapper = "bootstrapper";

    private static HashMap<String, String> getDefaults() {
        HashMap<String, String> map = new HashMap(5);
        map.put(optionCacheSize, "60");
        map.put(optionMaxConnections, "20");
        map.put(optionBootstrapper, "131.159.20.52:4433");
        map.put(optionListenAddress, "127.0.0.1:4433");
        map.put(optionApiAddress, "127.0.0.1:7001");
        return map;
    }

    public GossipConfigurationImpl(String filename) throws IOException {
        super(filename, "gossip", getDefaults());
    }

    @Override
    public Peer getBootstrapper() throws NoSuchElementException {
        InetSocketAddress address;
        address = this.getAddress("bootstrapper");
        return new Peer(address);
    }

    @Override
    public int getCacheSize() throws NoSuchElementException {
        try {
            return this.parser.getInt(section, "cache_size");
        } catch (ConfigParserException ex) {
            throw new NoSuchElementException(ex.getMessage());
        }
    }

    @Override
    public int getMaxConnections() throws NoSuchElementException {
        try {
            return this.parser.getInt(section, "max_connections");
        } catch (ConfigParserException ex) {
            throw new NoSuchElementException(ex.getMessage());
        }
    }
}
