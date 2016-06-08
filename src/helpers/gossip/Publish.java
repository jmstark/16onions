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
package helpers.gossip;

import gossip.api.AnnounceMessage;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
import protocol.MessageSizeExceededException;

/**
 * Class to connect to a Gossip module and publish a message
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class Publish {

    private static final Logger LOGGER = Logger.getLogger(
            "helpers.gossip.Publish");
    public static final int DATATYPE = 7881;
    private static final AtomicBoolean inShutdown = new AtomicBoolean();

    private static InetSocketAddress api_address;
    private static Thread shutdownThread;
    private static AsynchronousChannelGroup group;
    private static Connection connection;
    private static String message; //the message to publish
    private static boolean success;

    private static Map<String, String> getDefaultConfig() {
        HashMap<String, String> map = new HashMap(5);
        map.put("api_address", "127.0.0.1:7001");
        return map;
    }

    private static void printHelp(HelpFormatter formatter, Options options,
            String header) {
        formatter.printHelp("helpers.gossip.Publish",
                header,
                options,
                "Please report bugs to totakura@net.in.tum.de",
                true);
    }

    /**
     * Generate options for config (-c, --config )and help (-h, --help)
     *
     * @return the options object
     */
    public static Options defaultOptions() {
        Options options = new Options();
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
        return options;
    }

    /**
     * Parses the given arguments for default options (-c, -h).
     *
     * @param options the options. They may contain options additional to
     * default options
     * @param args the command line arguments
     * @return the commandLine object which can be used to read the parsed
     * argument values
     */
    public static CommandLine defaultConfigure(Options options, String[] args) {
        DefaultParser cli_parser;
        CommandLine commandline;
        HelpFormatter formatter;

        formatter = new HelpFormatter();
        cli_parser = new DefaultParser();

        try {
            commandline = cli_parser.parse(options, args);
        } catch (ParseException exp) {
            printHelp(formatter, options,
                    "Insufficient or wrong arguments given");
            System.exit(1);
            return null;
        }
        if (commandline.hasOption('h')) {
            printHelp(formatter, options, "Publishes a message through Gossip");
            System.exit(1);
        }
        return commandline;
    }

    /**
     * Read the config file
     *
     * @param commandline the object to parse the config filename
     * @return ConfigParser object
     */
    public static ConfigParser parseConfig(CommandLine commandline) {
        String config_filename = commandline.getOptionValue('c', "gossip.conf");
        ConfigParser parser = new ConfigParser(getDefaultConfig());
        try {
            parser.read(config_filename);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Unable to read config file: {0}",
                    config_filename);
            System.exit(1);
        }
        return parser;
    }

    static void configure(ConfigParser parser, CommandLine commandline) {
        String section = "gossip";
        String server_addr_str;
        try {
            server_addr_str = parser.get(section, "api_address");
        } catch (ConfigParserException ex) {
            LOGGER.severe(ex.toString());
            System.exit(1);
            return;
        }
        try {
            api_address = tools.Misc.fromAddressString(server_addr_str);
        } catch (URISyntaxException ex) {
            throw new RuntimeException("Invalid Gossip API address");
        }
    }

    private static void configure(Options options, String[] args) {

        options.addOption(Option.builder("m")
                .required(true)
                .hasArg(true)
                .longOpt("msg")
                .desc("message to publish through Gossip")
                .argName("MESSAGE")
                .build());

        message = commandline.getOptionValue('m');
        LOGGER.log(Level.FINE, "Attempting to publish message: {0}", message);
    }

    private static void await() {
        boolean terminated;

        shutdownThread = new Thread() {
            @Override
            public void run() {
                if (!inShutdown.compareAndSet(false, true)) {
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
                terminated = group.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                break;
            }
            if (terminated) {
                break;
            }
        } while (true);
    }

    private static void shutdown() {
        inShutdown.set(true);
        if (null != connection) {
            connection.disconnect();
        }
        LOGGER.fine("shutting down...");
        group.shutdown();
    }

    private static void sendMessage() {
        byte[] data = message.getBytes(Charset.forName("UTF-8"));
        AnnounceMessage announce;
        try {
            announce = new AnnounceMessage((short) 255, DATATYPE, data);
        } catch (MessageSizeExceededException ex) {
            LOGGER.log(Level.WARNING,
                    "Message is too long to announce, try shortening");
            shutdown();
            return;
        }
        connection.sendMsg(announce);
        success = true;
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(Publish.class.getName()).
                    log(Level.SEVERE, null, ex);
        }
        LOGGER.info("Message announced to Gossip");
        shutdown();
    }

    public static void main(String[] args) throws IOException {
        AsynchronousSocketChannel channel;

        configure(args);
        try {
            group = AsynchronousChannelGroup
                    .withFixedThreadPool(1, Executors.defaultThreadFactory());
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Please report this bug:\n{0}", ex);
            System.exit(1);
            return;
        }
        channel = AsynchronousSocketChannel.open(group);
        channel.connect(api_address, channel, new ConnectHandler());
        await();
    }

    private static class ConnectHandler implements
            CompletionHandler<Void, AsynchronousSocketChannel> {

        @Override
        public void completed(Void arg0, AsynchronousSocketChannel channel) {
            connection = new Connection(channel, new DisconnectHandler(null) {
                @Override
                protected void handleDisconnect(Object closure) {
                    if (!success) {
                        LOGGER.log(Level.SEVERE, "Publishing failed");
                    }
                }
            });
            sendMessage();
        }

        @Override
        public void failed(Throwable arg0, AsynchronousSocketChannel arg1) {
            LOGGER.log(Level.SEVERE, "Cannot connect to Gossip API");
            shutdown();
        }
    }
}
