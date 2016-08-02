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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
@RunWith(Parameterized.class)
public class ConfigurationImplTest extends ConfigurationImpl {

    // The various parameters used to run this test
    @Parameters(name = "{index}: {0}")
    public static Iterable<String> data() {
        return Arrays.asList("section1", "skyfall");
    }

    private static Map<String, String> defaults;

    /**
     * Constructor
     *
     * @param section the test parameter
     * @throws IOException
     */
    public ConfigurationImplTest(String section) throws IOException {
        super("test/tools/config/test.config",
                section,
                defaults);
    }

    @BeforeClass
    public static void setUpClass() {
        defaults = new HashMap(5);
        defaults.put("listen_address", "default");
        defaults.put("api_address", "default");
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getAddress method, of class ConfigurationImpl.
     */
    @Test
    public void testGetAddress() {
        System.out.println("getAddress");
        InetSocketAddress result = this.getAddress("api_address");
    }

    /**
     * Test of getListenAddress method, of class ConfigurationImpl.
     */
    @Test
    public void testGetListenAddress() {
        System.out.println("getListenAddress");
        InetSocketAddress result = this.getListenAddress();
    }

    /**
     * Test of getAPIAddress method, of class ConfigurationImpl.
     */
    @Test
    public void testGetAPIAddress() {
        System.out.println("getAPIAddress");
        InetSocketAddress result = this.getAPIAddress();
    }

}
