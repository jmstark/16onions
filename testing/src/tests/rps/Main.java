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
/**
 * Main class for the RPS tester
 *
 * @author totakura
 */
package tests.rps;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import org.apache.commons.cli.CommandLine;
import protocol.Connection;
import protocol.DisconnectHandler;
import rps.RpsConfiguration;
import rps.RpsConfigurationImpl;
import util.Program;
import util.config.CliParser;

public class Main extends Program {

    private InetSocketAddress api_address;
    private Connection connection;
    private Context context;

    Main() {
        super("tests.rps", "API conformance test case for RPS");
    }

    @Override
    protected void parseCommandLine(CommandLine cli, CliParser parser) {
        String filename = parser.getConfigFilename("rps.conf");
        RpsConfiguration config;
        try {
            config = new RpsConfigurationImpl(filename);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read config file");
        }
        api_address = config.getAPIAddress();
    }

    @Override
    protected void cleanup() {
        if (null != connection) {
            connection.disconnect();
            connection = null;
        }
        if (null != context) {
            context.shutdown();
        }
    }

    @Override
    protected void run() {
        AsynchronousSocketChannel channel;
        try {
            channel = AsynchronousSocketChannel.open(this.group);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        channel.connect(api_address, channel, new ConnectCompletion());
    }

    private class ConnectCompletion implements
            CompletionHandler<Void, AsynchronousSocketChannel> {

        @Override
        public void completed(Void none, AsynchronousSocketChannel channel) {
            connection = new Connection(channel, new DisconnectHandler(null) {
                @Override
                protected void handleDisconnect(Object closure) {
                    if (!Main.this.inShutdown()) {
                        logger.warning("Connection disconnected");
                        connection = null;
                        shutdown();
                    }
                }
            });
            logger.fine("Connected to RPS API");
            context = new Context(connection, scheduledExecutor, logger);
            connection.receive(context);
        }

        @Override
        public void failed(Throwable arg0, AsynchronousSocketChannel arg1) {
            logger.severe("Could not connect to RPS API");
            shutdown();
        }

    }

    public static void main(String[] args) throws IOException {
        Main mockup = new Main();
        mockup.start(args);
    }

}
