/*
 * Copyright (C) 2016 totakura
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
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.ini4j.ConfigParser;
import protocol.ProtocolServer;
import tests.auth.Context;
import util.Program;
import util.config.CliParser;

/**
 *
 * @author totakura
 */
public class Main extends Program {

    static Logger LOGGER;
    private InetSocketAddress apiAddress;
    private Context context;
    private ProtocolServer server;
    private NseMockupConfiguration config;

    public Main() {
        super("mockups.nse", "Mockup module for NSE");
    }

    @Override
    protected void parseCommandLine(CommandLine cli, CliParser parser) {
        String filename = parser.getConfigFilename("nse.conf");

        try {
            config = new NseMockupConfiguration(filename);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read config file");
        }
    }

    @Override
    protected void cleanup() {
        if (null == server) {
            return;
        }
        try {
            server.stop();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    protected void run() {
        try {
            server = new NseApiServer(config, this.group);
        } catch (IOException | ConfigParser.NoSectionException |
                ConfigParser.NoOptionException |
                ConfigParser.InterpolationException ex) {
            LOGGER.severe(ex.getLocalizedMessage());
            shutdown();
            throw new RuntimeException("Cannot start API server; cannot continue");
        }
        server.start();
        LOGGER.info("Started API server");
    }

    public static void main(String[] args) throws IOException {
        Main mockup = new Main();
        LOGGER = mockup.logger;
        mockup.start(args);
    }
}
