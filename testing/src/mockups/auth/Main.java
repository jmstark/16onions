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
package mockups.auth;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;
import auth.OnionAuthConfiguration;
import org.apache.commons.cli.CommandLine;
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

    public Main() {
        super("mockups.auth", "Mockup module for OnionAuth");
    }

    @Override
    protected void parseCommandLine(CommandLine cli, CliParser parser) {
        String filename = parser.getConfigFilename("auth.conf");
        OnionAuthConfiguration config;
        try {
            config = new OnionAuthConfiguration(filename);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Unable to read config file: {0}", ex.
                    getLocalizedMessage());
            Runtime.getRuntime().exit(util.ExitStatus.CONF_ERROR);
            return;
        }
        apiAddress = config.getAPIAddress();
    }

    @Override
    protected void cleanup() {
        try {
            server.stop();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    protected void run() {
        try {
            server = new AuthApiServer(apiAddress, this.group);
        } catch (IOException ex) {
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
