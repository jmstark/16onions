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
package gossip;

import gossip.api.ApiServer;
import gossip.p2p.Client;
import gossip.p2p.GossipServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import tools.config.CliParser;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger("Gossip");
    //private static GossipConfiguration config;
    private static int cache_size;
    private static int max_connections;
    private static Cache cache;
    private static Peer bootstrapper;
    private static InetSocketAddress listen_address;
    private static InetSocketAddress api_address;
    private static GossipServer p2p_server;
    private static ApiServer api_server;
    private static AsynchronousChannelGroup group;
    private static ScheduledExecutorService scheduled_executor;
    private static ScheduledFuture future_overlay;

    private static void printHelp(HelpFormatter formatter, Options options, String header) {
        formatter.printHelp("gossip.Main",
                header,
                options,
                "Please report bugs to totakura@net.in.tum.de",
                true);
    }

    private static void configure(String[] args) {
        CommandLine commandline;
        CliParser parser;
        parser = new CliParser("gossip.Main",
                "A sample (and insecure) implementation of Gossip");
        commandline = parser.parse(args);
        String filename = commandline.getOptionValue('c', "gossip.conf");
        GossipConfiguration config;
        try {
            config = new GossipConfigurationImpl(filename);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Unable to read config file: {0}",
                    filename);
            throw new RuntimeException();
        }
        cache_size = config.getCacheSize();
        max_connections = config.getMaxConnections();
        bootstrapper = config.getBootstrapper();
        listen_address = config.getListenAddress();
        api_address = config.getAPIAddress();
        LOGGER.log(Level.FINE, "Creating cache with {0} entries", cache_size);
        cache = Cache.initialize(cache_size);
    }

    private static void startServer() {
        try {
            group = AsynchronousChannelGroup
                    .withFixedThreadPool(1, Executors.defaultThreadFactory());
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Please report this bug:\n{0}", ex);
            System.exit(1);
            return;
        }
        scheduled_executor = Executors.newScheduledThreadPool(
                (Runtime.getRuntime().availableProcessors() > 1) ? 2 : 1);
        //start p2p server
        try {
            p2p_server = new GossipServer(listen_address,
                    group, scheduled_executor, max_connections);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "P2P server failed to initialize: {0}",
                    ex.toString());
            System.exit(1);
        }
        p2p_server.start();
        LOGGER.fine("P2P server started");
        //start api server
        try {
            api_server = new ApiServer(api_address, group);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "API server failed to initialize: {0}",
                    ex.toString());
            System.exit(1);
        }
        api_server.start();
        LOGGER.fine("API server started");
    }

    private static void bootstrap() {
        if (null == bootstrapper) {
            LOGGER.log(Level.INFO,
                    "We are the bootstrap peer");
            return;
        }
        try {
            new Client(bootstrapper, listen_address, group, scheduled_executor);
        } catch (IOException ex) {
            throw new RuntimeException("Cannot connect to bootstrap peer");
        }
    }

    /**
     * Start a periodic task to maintain overlay connections
     */
    private static void maintainOverlay() {
        future_overlay = scheduled_executor.scheduleWithFixedDelay(
                new Runnable() {
            /**
             * Connect to a minimum number of peers
             */
            @Override
                    public void run() {
                        int connected = 0;
                LOGGER.log(Level.FINE, "Starting topology maintenance");
                Iterator<Peer> iter = cache.peerIterator();
                while (iter.hasNext()) {
                    Peer peer = iter.next();
                    if (peer.isConnected()) {
                        connected++;
                    }
                }
                if (connected >= max_connections) {
                    LOGGER.log(Level.FINE, "We maxed out our connections");
                    return;
                }
                iter = cache.peerIterator();
                while (iter.hasNext() && (connected < max_connections)) {
                    Peer peer = iter.next();
                    if (peer.isConnected()) {
                        continue;
                    }
                    try {
                        new Client(peer, listen_address, group,
                                scheduled_executor);
                    } catch (IOException ex) {
                        LOGGER.log(Level.WARNING,
                                "Connecting to new peer {0} failed: {1}",
                                new Object[]{peer, ex.toString()});
                    }
                }
            }
        }, 5, 30, TimeUnit.SECONDS);
    }

    private static void await() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    p2p_server.stop();
                    api_server.stop();
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "Stopping servers failed: {0}",
                            ex.toString());
                    LOGGER.log(Level.INFO, "You may have to kill the process");
                }
                future_overlay.cancel(true);
                group.shutdown();
                scheduled_executor.shutdown();
                LOGGER.info("Shutting down; this may take a while...");
            }
        });
        boolean terminated = false;
        do {
            try {
                terminated = scheduled_executor.awaitTermination(5,
                        TimeUnit.MINUTES);
                terminated &= group.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                //do nothing
            }
        } while (!terminated);
    }

    public static void main(String[] args) {
        configure(args);
        startServer();
        bootstrap();
        maintainOverlay();
        LOGGER.info("All systems up and running. Send SIGINT, SIGTERM or press"
                + " Ctrl+C to initiate shutdown");
        await();
    }
}
