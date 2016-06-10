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
package tools.config;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.NoSuchElementException;
import org.ini4j.ConfigParser;
import org.ini4j.ConfigParser.ConfigParserException;
import tools.Misc;

abstract public class ConfigurationImpl implements Configuration {

    protected static final String OPTION_LISTEN_ADDRESS = "listen_address";
    protected static final String OPTION_API_ADDRESS = "api_address";
    protected final ConfigParser parser;
    protected final String section;
    protected ConfigurationImpl(String filename,
            String section,
            Map<String, String> defaults)
            throws IOException {
        this.section = section;
        this.parser = new ConfigParser(defaults);
        this.parser.read(filename);
    }

    protected InetSocketAddress getAddress(String option) throws
            NoSuchElementException {
        InetSocketAddress address;
        String value;

        try {
            value = parser.get(this.section, option);
        } catch (ConfigParserException ex) {
            throw new NoSuchElementException(ex.getMessage());
        }
        try {
            address = Misc.fromAddressString(value);
        } catch (URISyntaxException ex) {
            throw new NoSuchElementException(ex.getMessage());
        }
        return address;
    }

    @Override
    public InetSocketAddress getListenAddress() throws
            NoSuchElementException {
        return getAddress(OPTION_LISTEN_ADDRESS);
    }

    @Override
    public InetSocketAddress getAPIAddress() throws
            NoSuchElementException {
        return getAddress(OPTION_API_ADDRESS);
    }

}
