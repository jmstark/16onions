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

import gossip.GossipConfiguration;
import gossip.GossipConfigurationImpl;
import gossip.api.AnnounceMessage;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import protocol.Connection;
import protocol.DisconnectHandler;
import protocol.MessageSizeExceededException;
import tools.Program;
import tools.config.CliParser;

/**
 * Class to connect to a Gossip module and publish a message
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public final class Publish extends Program {

    public static final int DATATYPE = 7881;
    private static InetSocketAddress api_address;
    private static Connection connection;
    private static String message; //the message to publish
    private static boolean success;

    public Publish() {
        super("helpers.gossip.Publish", "Publishes a message through Gossip");
    }

    private void sendMessage() {
        byte[] data = message.getBytes(Charset.forName("UTF-8"));
        AnnounceMessage announce;
        try {
            announce = new AnnounceMessage((short) 255, DATATYPE, data);
        } catch (MessageSizeExceededException ex) {
            logger.log(Level.WARNING,
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
        logger.info("Message announced to Gossip");
        shutdown();
    }

    public static void main(String[] args) throws IOException {
        new Publish().start(args);
    }

    @Override
    protected void addParserOptions(CliParser parser) {
        parser.addOption(Option.builder("m")
                .required(true)
                .hasArg(true)
                .longOpt("msg")
                .desc("message to publish through Gossip")
                .argName("MESSAGE")
                .build());
    }

    @Override
    protected void parseCommandLine(CommandLine cli, CliParser parser) {
        String filename = parser.getConfigFilename("gossip.conf");
        GossipConfiguration config;
        try {
            config = new GossipConfigurationImpl(filename);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read config file" + filename);
        }
        api_address = config.getAPIAddress();
        message = cli.getOptionValue('m');
        logger.log(Level.FINE, "Attempting to publish message: {0}", message);
    }

    @Override
    protected void cleanup() {
        if (null != connection) {
            connection.disconnect();
            connection = null;
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
        channel.connect(api_address, channel, new ConnectHandler());
    }

    private class ConnectHandler implements
            CompletionHandler<Void, AsynchronousSocketChannel> {

        @Override
        public void completed(Void arg0, AsynchronousSocketChannel channel) {
            connection = new Connection(channel, new DisconnectHandler(null) {
                @Override
                protected void handleDisconnect(Object closure) {
                    if (!success) {
                        logger.log(Level.SEVERE, "Publishing failed");
                    }
                }
            });
            sendMessage();
        }

        @Override
        public void failed(Throwable arg0, AsynchronousSocketChannel arg1) {
            logger.log(Level.SEVERE, "Cannot connect to Gossip API");
            shutdown();
        }
    }
}
