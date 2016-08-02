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
import static java.lang.Math.max;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import nse.api.QueryMessage;
import protocol.Connection;
import protocol.DisconnectHandler;
import tools.config.CliParser;

/**
 * Main class for testing Main API
 *
 * @author totakura
 */
public class Main {

    private static Context context;
    private static AsynchronousChannelGroup channelGroup;
    private static ScheduledExecutorService scheduledExecutor;
    private static Connection connection;
    private static Thread shutdownThread;
    private static final Logger LOGGER = Logger.getLogger("tests.nse");
    private static final AtomicBoolean IN_SHUTDOWN = new AtomicBoolean();

    static Logger getLogger() {
        return LOGGER;
    }

    private static void shutdown() {
        IN_SHUTDOWN.set(true);
        if (null != connection) {
            connection.disconnect();
        }
        LOGGER.fine("shutting down...");
        channelGroup.shutdown();
    }

    private static void await() {
        boolean terminated;

        shutdownThread = new Thread() {
            @Override
            public void run() {
                if (!IN_SHUTDOWN.compareAndSet(false, true)) {
                    return;
                }
                LOGGER.log(Level.INFO,
                        "Shutting down; this may take a while...");
                shutdown();
            }
        };

        Runtime.getRuntime().addShutdownHook(shutdownThread);
        do {
            try {
                terminated = channelGroup.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                break;
            }
            if (terminated) {
                break;
            }
        } while (true);
    }

    public static void main(String args[]) throws IOException {
        CliParser parser = new CliParser("helpers.gossip.Notify",
                "Notifies whenever a message published through Gossip is seen.");
        parser.parse(args);
        String filename = parser.getConfigFilename("gossip.conf");
        NseConfiguration config;
        try {
            config = new NseConfiguration(filename);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read config file");
        }
        InetSocketAddress api_address = config.getAPIAddress();

        //start the test by connecting to the API socket
        // start the test by opening as many connections as possible
        final int cores = Runtime.getRuntime().availableProcessors();
        final ThreadFactory threadFactory = Executors.defaultThreadFactory();
        channelGroup = AsynchronousChannelGroup.withFixedThreadPool(
                max(1, cores - 1), threadFactory);
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);

        context = new ContextImpl();
        AsynchronousSocketChannel channel;
        channel = AsynchronousSocketChannel.open(channelGroup);
        channel.connect(api_address, channel, new ConnectCompletion());
        await();
    }

    static private class ConnectCompletion
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
                    if (!IN_SHUTDOWN.get()) {
                        LOGGER.log(Level.WARNING, "Connection disconnected");
                        connection = null;
                        shutdown();
                    }
                }
            });
            connection.receive(new ApiMessageHandler(context));
            scheduleNextQuery(0);
        }

        @Override
        public void failed(Throwable arg0, AsynchronousSocketChannel arg1) {
            LOGGER.log(Level.SEVERE, "Cannot connect to NSE API");
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
