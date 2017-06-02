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
package tests.auth;

import auth.OnionAuthConfiguration;
import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.interfaces.RSAPublicKey;
import org.ini4j.ConfigParser;
import util.PEMParser;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class OnionAuthTesterConfiguration extends OnionAuthConfiguration {

    public OnionAuthTesterConfiguration(String filename) throws IOException {
        super(filename);
    }

    public RSAPublicKey getHostkey(String option) throws
            ConfigParser.NoSectionException, ConfigParser.NoOptionException,
            ConfigParser.InterpolationException, IOException,
            InvalidKeyException {
        String filename;
        filename = this.parser.get(this.section, option);
        return PEMParser.getPublicKeyFromPEM(new File(filename));
    }

}
