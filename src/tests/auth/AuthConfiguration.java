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

import java.io.IOException;
import java.util.HashMap;
import tools.config.ConfigurationImpl;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
class AuthConfiguration extends ConfigurationImpl {

    protected static final String OPTION_HOSTKEY = "hostkey";

    private static HashMap<String, String> getDefaults() {
        HashMap<String, String> map = new HashMap(5);
        map.put(OPTION_API_ADDRESS, "127.0.0.1:7004");
        return map;
    }

    AuthConfiguration(String filename) throws IOException {
        super(filename, "auth", getDefaults());
    }
}
