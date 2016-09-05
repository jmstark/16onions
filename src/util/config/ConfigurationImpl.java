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

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.NoSuchElementException;
import org.ini4j.Ini;
import util.Misc;

/**
 * Default implementation of the Configuration interface
 *
 * @author totakura
 */
abstract public class ConfigurationImpl implements Configuration {

    protected static final String OPTION_LISTEN_ADDRESS = "listen_address";
    protected static final String OPTION_API_ADDRESS = "api_address";
    protected final Ini parser;
    protected final String section;
    protected ConfigurationImpl(String filename,
            String section,
            Map<String, String> defaults)
            throws IOException {
        this.section = section;
        this.parser = new Ini(new File(filename));
    }

    @Override
    public InetSocketAddress getAddress(String option) throws
            NoSuchElementException {
        InetSocketAddress address;
        String value;

        value = parser.get(this.section, option);
        if (null == value) {
            throw new NoSuchElementException(
                    MessageFormat.format("{0} not found in section {1}",
                            new Object[]{option, section}));
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
