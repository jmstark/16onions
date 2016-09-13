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

import gossip.GossipConfiguration;
import gossip.GossipConfigurationImpl;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.InvalidKeyException;
import java.security.interfaces.RSAPublicKey;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import onion.OnionConfiguration;
import onion.OnionConfigurationImpl;
import org.ini4j.ConfigParser;
import util.config.ConfigurationImpl;

public class RpsConfigurationImpl extends ConfigurationImpl implements
        RpsConfiguration {

    private static final String OPTION_PUBLISH_INTERVAL = "10";
    private static final Map<String, String> DEFAULTS;
    private final GossipConfiguration gossipConfig;
    private final OnionConfiguration onionConfig;

    static {
        DEFAULTS = new HashMap(10);
        DEFAULTS.put(OPTION_LISTEN_ADDRESS, "127.0.0.1:6101");
        DEFAULTS.put(OPTION_API_ADDRESS, "127.0.0.1:7101");
    }

    public RpsConfigurationImpl(String filename) throws IOException {
        super(filename, "rps", DEFAULTS);
        this.gossipConfig = new GossipConfigurationImpl(filename);
        this.onionConfig = new OnionConfigurationImpl(filename);
    }

    @Override
    public int getPublishInterval() throws NoSuchElementException {
        String option = OPTION_PUBLISH_INTERVAL;
        try {
            return this.parser.getInt(this.section, option);
        } catch (ConfigParser.NoSectionException |
                ConfigParser.NoOptionException |
                ConfigParser.InterpolationException ex) {
            throw new NoSuchElementException(MessageFormat.format(
                    "Option {0} not found in section {1}", option, this.section));
        }
    }

    @Override
    public InetSocketAddress getGossipAPIAddress() throws NoSuchElementException {
        return this.gossipConfig.getAPIAddress();
    }

    @Override
    public InetSocketAddress getOnionP2PAddress() throws NoSuchElementException {
        return this.onionConfig.getListenAddress();
    }

    @Override
    public RSAPublicKey getHostKey()
            throws NoSuchElementException, IOException, InvalidKeyException {
        return this.onionConfig.getHostKey();
    }
}
