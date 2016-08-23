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
package tests;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import onionauth.OnionAuthConfiguration;
import org.apache.commons.cli.CommandLine;
import protocol.Connection;
import protocol.DisconnectHandler;
import tools.Program;
import tools.config.CliParser;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class OnionAuthTest extends Program {

    private OnionAuthConfiguration config;
    private InetSocketAddress api_address;
    private Connection connection;

    public OnionAuthTest() {
        super("OnionAuthTest", "OnionAuth API compliance tester");
    }

    @Override
    protected void parseCommandLine(CommandLine cli, CliParser parser) {
        String configFileName;
        configFileName = parser.getConfigFilename("default.config");
        try {
            config = new OnionAuthConfiguration(configFileName);
        } catch (IOException ex) {
            throw new RuntimeException(
                    "Unable to read config file: " + configFileName);
        }
        api_address = config.getAPIAddress();
    }

    @Override
    protected void cleanup() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void run() {
        AsynchronousSocketChannel channel;

        try {
            channel = AsynchronousSocketChannel.open(this.group);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        channel.connect(api_address, channel, new ConnectHandler());

    }

    public static void Main(String args[]) throws IOException {
        new OnionAuthTest().start(args);
    }

    private class ConnectHandler implements
            CompletionHandler<Void, AsynchronousSocketChannel> {

        @Override
        public void completed(Void arg0, AsynchronousSocketChannel channel) {
            logger.log(Level.INFO, "Connected to API socket");
            connection = new Connection(channel, new DisconnectHandler(null) {
                @Override
                protected void handleDisconnect(Object closure) {
                    if (OnionAuthTest.this.inShutdown()) {
                        return;
                    }
                    logger.log(Level.SEVERE, "Connection disconnected");
                }
            });
        }

        @Override
        public void failed(Throwable arg0, AsynchronousSocketChannel channel) {
            logger.log(Level.SEVERE, "Cannot connect to API socket");
            shutdown();
        }

    }

}
