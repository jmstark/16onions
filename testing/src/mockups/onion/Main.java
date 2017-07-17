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
import java.security.InvalidKeyException;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import mockups.onion.api.OnionApiServer;
import mockups.onion.p2p.OnionP2pServer;
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

    static public Logger LOGGER;

    private OnionConfigurationImpl config;
    private InetSocketAddress rpsAddress;
    private ProtocolServer apiServer;
    private ProtocolServer p2pServer;
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
            LOGGER.log(Level.SEVERE, "Unable to read config file: {0}", ex.
                    getLocalizedMessage());
            Runtime.getRuntime().exit(util.ExitStatus.CONF_ERROR);
            return;
        }
        rpsAddress = rpsConfig.getAPIAddress();
    }

    @Override
    protected void cleanup() {
        if (null != apiServer) {
            try {
                apiServer.stop();
            } catch (IOException ex) {
                logger.warning("failed to stop the API server");
            }
        }
        if (null != p2pServer) {
            try {
                p2pServer.stop();
            } catch (IOException ex) {
                logger.warning("failed to stop the P2P server");
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
            try {
                // create Onion API apiServer
                apiServer = new OnionApiServer(config, group);
            } catch (NoSuchElementException | InvalidKeyException ex) {
                throw new RuntimeException(ex);
            }
        } catch (IOException ex) {
            LOGGER.severe("Could not start API server; quitting");
            shutdown();
            return;
        }
        try {
            p2pServer = new OnionP2pServer(config, group);
        } catch (IOException ex) {
            LOGGER.severe("Could not start P2p server; quitting");
            shutdown();
            return;
        }
        apiServer.start();
        p2pServer.start();
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
