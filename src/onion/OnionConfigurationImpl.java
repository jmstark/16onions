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
package onion;

import java.io.File;
import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import org.ini4j.ConfigParser;
import util.config.ConfigurationImpl;

public class OnionConfigurationImpl extends ConfigurationImpl implements
        OnionConfiguration {

    private static final String OPTION_HOSTKEY = "hostkey";
    private static final Map<String, String> DEFAULTS;

    static {
        DEFAULTS = new HashMap(10);
        DEFAULTS.put(OPTION_LISTEN_ADDRESS, "127.0.0.1:6301");
        DEFAULTS.put(OPTION_API_ADDRESS, "127.0.0.1:7301");
        DEFAULTS.put(OPTION_HOSTKEY, "hostkey.pem");
    }
    public OnionConfigurationImpl(String filename) throws IOException {
        super(filename, "onion", null);
    }

    @Override
    public RSAPublicKey getHostKey() throws NoSuchElementException {
        String filename;
        String option = OPTION_HOSTKEY;
        try {
            filename = this.parser.get(this.section, option);
        } catch (ConfigParser.NoSectionException |
                ConfigParser.NoOptionException |
                ConfigParser.InterpolationException ex) {
            throw new NoSuchElementException(MessageFormat.format(
                    "Option {0} not found in section {1}", option, this.section));
        }
        return new File(filename);
    }

}
