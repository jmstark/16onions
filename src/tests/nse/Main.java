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
package tests.nse;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import nse.NseConfiguration;
import nse.api.QueryMessage;
import org.apache.commons.cli.CommandLine;
import protocol.Connection;
import protocol.DisconnectHandler;
import util.Program;
import util.config.CliParser;

/**
 * Main class for testing Main API
 *
 * @author totakura
 */
public class Main extends Program {

    private Context context;
    private Connection connection;
    private InetSocketAddress api_address;

    Main() {
        super("tests.nse", "API conformance test case for NSE");
    }

    Logger getLogger() {
        return this.logger;
    }

    @Override
    protected void cleanup() {
        if (null != connection) {
            connection.disconnect();
            connection = null;
        }
    }

    @Override
    protected void parseCommandLine(CommandLine cli, CliParser parser) {
        String filename = parser.getConfigFilename("nse.conf");
        NseConfiguration config;
        try {
            config = new NseConfiguration(filename);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read config file");
        }
        api_address = config.getAPIAddress();
    }

    @Override
    protected void run() {
        context = new ContextImpl();
        AsynchronousSocketChannel channel;
        try {
            channel = AsynchronousSocketChannel.open(this.group);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        channel.connect(api_address, channel, new ConnectCompletion());
    }

    public static void main(String args[]) throws IOException {
        new Main().start(args);
    }

    private class ConnectCompletion
            implements CompletionHandler<Void, AsynchronousSocketChannel> {
        private final Random random;

        private
        ConnectCompletion() {
            this.random = new Random();
        }

        @Override
        public void completed(Void arg0, AsynchronousSocketChannel channel) {
            connection = new Connection(channel, new DisconnectHandler(null) {
                @Override
                protected void handleDisconnect(Object closure) {
                    if (!Main.this.inShutdown()) {
                        logger.log(Level.WARNING, "Connection disconnected");
                        connection = null;
                        shutdown();
                    }
                }
            });
            connection.receive(new ApiMessageHandler(context, logger));
            scheduleNextQuery(0);
        }

        @Override
        public void failed(Throwable arg0, AsynchronousSocketChannel arg1) {
            logger.log(Level.SEVERE, "Cannot connect to NSE API");
            shutdown();
        }

        /**
         * Schedules the next query.
         *
         * @param delay delay in milliseconds after which the next query should
         * be made.
         */
        private void scheduleNextQuery(int delay) {
            scheduledExecutor.schedule(new Runnable() {
                @Override
                public void run() {
                    QueryMessage query;
                    int delay;

                    query = new QueryMessage();
                    connection.sendMsg(query);
                    context.sentQuery();
                    delay = random.nextInt(30 * 1000); // 30 seconds
                    scheduleNextQuery(delay);
                }

            }, delay, TimeUnit.MILLISECONDS);
        }
    }
}
