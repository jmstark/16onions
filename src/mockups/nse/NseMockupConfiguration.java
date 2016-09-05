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
package mockups.nse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import nse.NseConfiguration;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class NseMockupConfiguration extends NseConfiguration {

    private static final Map<String, String> defaults;
    private static final String OPTION_MOCKUP_ESTIMATE_MAX
            = "mockup_estimate_max";
    private static final String OPTION_MOCKUP_DEVIATION_MAX
            = "mockup_deviation_max";

    static {
        defaults = new HashMap(5);
        defaults.put(OPTION_MOCKUP_ESTIMATE_MAX, Integer.toString(100000));
        defaults.put(OPTION_MOCKUP_DEVIATION_MAX, Integer.toString(5));
    }

    public NseMockupConfiguration(String filename) throws IOException {
        super(filename, defaults);
    }

    public int getMockupMaxEstimate() {
        return Integer.parseInt(this.parser.get(this.section,
                OPTION_MOCKUP_ESTIMATE_MAX));
    }

    public int getMockupMaxDeviation() {
        return Integer.parseInt(this.parser.get(this.section,
                OPTION_MOCKUP_DEVIATION_MAX));
    }

}
