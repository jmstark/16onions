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

import gossip.p2p.GossipMessageHandler;
import gossip.p2p.PeerContext;
import gossip.p2p.GossipServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.ini4j.ConfigParser;
import org.ini4j.ConfigParser.ConfigParserException;
import protocol.Connection;
import protocol.DisconnectHandler;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class Main {

    private static final Logger logger = Logger.getLogger("Gossip");
    private static int cache_size;
    private static int max_connections;
    private static Cache cache;
    private static Peer bootstrapper;
    private static InetSocketAddress listen_address;
    private static GossipServer server;
    private static AsynchronousChannelGroup group;
    private static ScheduledExecutorService scheduled_executor;

    private static void printHelp(HelpFormatter formatter, Options options, String header) {
        formatter.printHelp("gossip.Main",
                header,
                options,
                "Please report bugs to totakura@net.in.tum.de",
                true);
    }

    private static Map<String, String> getDefaultConfig() {
        HashMap<String, String> map = new HashMap(5);
        map.put("cache_size", "60");
        map.put("max_connections", "20");
        map.put("bootstrapper", "131.159.20.52:4433");
        map.put("listen_address", "127.0.0.1:4433");
        return map;
    }

    static InetSocketAddress fromAddressString(String address)
            throws URISyntaxException {
        URI uri;

        uri = new URI("gossip://" + address);
        String hostname = uri.getHost();
        int port = uri.getPort();
        return new InetSocketAddress(hostname, port);
    }

    private static void configure(String[] args) {
        DefaultParser cli_parser;
        CommandLine commandline;
        Options options;
        HelpFormatter formatter;

        options = new Options();
        options.addOption(Option.builder("c")
                .required(false)
                .longOpt("config")
                .desc("configuration file")
                .optionalArg(false)
                .argName("FILE")
                .hasArg().build());
        options.addOption(Option.builder("h")
                .required(false)
                .hasArg(false)
                .longOpt("help")
                .desc("show usage help")
                .build());
        formatter = new HelpFormatter();
        cli_parser = new DefaultParser();

        try {
            commandline = cli_parser.parse(options, args);
        } catch (ParseException exp) {
            printHelp(formatter, options, "Unable to parse arguments");
            System.exit(1);
            return;
        }
        if (commandline.hasOption('h')) {
            printHelp(formatter, options,
                    "A sample (and insecure) implementation of Gossip");
            System.exit(1);
        }
        String config_filename = commandline.getOptionValue('c', "gossip.conf");
        ConfigParser config_parser = new ConfigParser(getDefaultConfig());
        try {
            config_parser.read(config_filename);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to read config file: {0}",
                    config_filename);
            System.exit(1);
        }
        String section = "gossip";
        String bootstrapper_addr_str;
        String listen_addr_str;
        try {
            cache_size = config_parser.getInt(section, "cache_size");
            max_connections = config_parser.getInt(section, "max_connections");
            bootstrapper_addr_str = config_parser.get(section, "bootstrapper");
            listen_addr_str = config_parser.get(section, "listen_address");
        } catch (ConfigParserException ex) {
            logger.severe(ex.toString());
            System.exit(1);
            return;
        }
        InetSocketAddress bootstrapper_address;
        try {
            bootstrapper_address = fromAddressString(bootstrapper_addr_str);
        } catch (URISyntaxException ex) {
            throw new RuntimeException("Invalid address for bootstrapper");
        }
        try {
            listen_address = fromAddressString(listen_addr_str);
        } catch (URISyntaxException ex) {
            throw new RuntimeException("Invalid format for listen_address");
        }
        if (!bootstrapper_address.equals(listen_address)) {
            bootstrapper = new Peer(bootstrapper_address);
        }
        logger.log(Level.FINE,
                "Creating cache with {0} entries",
                max_connections / 2);
        cache = new Cache(max_connections / 2);
    }

    private static void startServer() {
        try {
            group = AsynchronousChannelGroup
                    .withFixedThreadPool(1, Executors.defaultThreadFactory());
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Please report this bug:\n{0}", ex);
            System.exit(1);
            return;
        }
        scheduled_executor = Executors.newScheduledThreadPool(
                (Runtime.getRuntime().availableProcessors() > 1) ? 2 : 1);
        try {
            server = new GossipServer(listen_address,
                    group, scheduled_executor, cache, max_connections);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Gossip service failed to initialize: {0}",
                    ex.toString());
            System.exit(1);
        }
        server.start();
    }

    private static void bootstrap() {
        AsynchronousSocketChannel channel;

        if (null == bootstrapper) {
            logger.log(Level.INFO,
                    "We are the bootstrap peer");
            return;
        }
        try {
            channel = AsynchronousSocketChannel.open(group);
        } catch (IOException ex) {
            throw new RuntimeException("Cannot connect to bootstrap peer");
        }
        channel.connect(bootstrapper.getAddress(), channel,
                new CompletionHandler<Void, AsynchronousSocketChannel>() {
            @Override
            public void completed(Void arg0, AsynchronousSocketChannel channel) {
                Connection connection;
                PeerContext context;

                context = new PeerContext(bootstrapper, scheduled_executor,
                        cache);
                connection = new Connection(channel,
                        new PeerDisconnectHandler(context));
                assert (!bootstrapper.isConnected());
                bootstrapper.setConnection(connection);
                context.sendHello(listen_address);
                connection.receive(new GossipMessageHandler(context, cache));
            }

            @Override
            public void failed(Throwable arg0, AsynchronousSocketChannel channel) {
                logger.log(Level.SEVERE, "Connection to bootstrapper failed");
                try {
                    channel.close();
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    private static void await() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    server.stop();
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, "Stopping server failed: {0}",
                            ex.toString());
                    logger.log(Level.INFO, "You may have to kill the process");
                }
                group.shutdown();
                scheduled_executor.shutdown();
                logger.log(Level.INFO, "Shutting down; this may take a while...");
            }
        });
        do {
            try {
                group.awaitTermination(1, TimeUnit.DAYS);
                scheduled_executor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                break;
            }
        } while (true);
    }

    private static class PeerDisconnectHandler
            extends DisconnectHandler<PeerContext> {

        public PeerDisconnectHandler(PeerContext closure) {
            super(closure);
        }

        @Override
        protected void handleDisconnect(PeerContext context) {
            context.shutdown();
            cache.removePeer(context.getPeer());
        }
    }

    public static void main(String[] args) {
        configure(args);
        startServer();
        bootstrap();
        await();
    }
}
