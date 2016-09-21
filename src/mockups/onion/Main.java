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
package mockups.onion;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import onion.OnionConfigurationImpl;
import org.apache.commons.cli.CommandLine;
import protocol.Connection;
import protocol.DisconnectHandler;
import protocol.ProtocolServer;
import rps.RpsConfigurationImpl;
import util.Program;
import util.config.CliParser;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class Main extends Program {

    static Logger LOGGER;

    private OnionConfigurationImpl config;
    private InetSocketAddress rpsAddress;
    private ProtocolServer server;
    private Connection rpsConnection;

    private Main() {
        super("mockups.onion.Main", "Mockup implementation for Onion module");
    }

    @Override
    protected void parseCommandLine(CommandLine cli, CliParser parser) {
        RpsConfigurationImpl rpsConfig;
        String filename = parser.getConfigFilename("onion.conf");
        try {
            config = new OnionConfigurationImpl(filename);
            rpsConfig = new RpsConfigurationImpl(filename);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read config file: " + ex.
                    getMessage());
        }
        rpsAddress = rpsConfig.getAPIAddress();
    }

    @Override
    protected void cleanup() {
        if (null != server) {
            try {
                server.stop();
            } catch (IOException ex) {
                logger.warning("failed to stop the API server");
            }
        }
        if (null != rpsConnection) {
            rpsConnection.disconnect();
        }
    }

    @Override
    protected void run() {
        AsynchronousSocketChannel channel;
        try {
            channel = AsynchronousSocketChannel.open(group);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        channel.connect(rpsAddress, channel, new RpsConnectHandler());
        try {
            // create Onion API server
            server = new OnionApiServer(config, group);
        } catch (IOException ex) {
            LOGGER.severe("Could not start API server; quitting");
            shutdown();
            return;
        }
        server.start();
    }

    private class RpsConnectHandler implements
            CompletionHandler<Void, AsynchronousSocketChannel> {

        @Override
        public void completed(Void arg0, AsynchronousSocketChannel channel) {
            rpsConnection = new Connection(channel,
                    new DisconnectHandler(null) {
                @Override
                protected void handleDisconnect(Object closure) {
                    logger.warning("API connection to RPS disconnected");
                    rpsConnection = null;
                    shutdown();
                }
            });
        }

        @Override
        public void failed(Throwable arg0, AsynchronousSocketChannel channel) {
            logger.warning("Could not connect to RPS API");
            shutdown();
        }
    }

    public static void main(String[] args) throws IOException {
        Main mockup = new Main();
        LOGGER = mockup.logger;
        mockup.start(args);
    }

}
