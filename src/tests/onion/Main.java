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
package tests.onion;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.security.InvalidKeyException;
import java.security.interfaces.RSAPublicKey;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import onion.OnionConfiguration;
import onion.OnionConfigurationImpl;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import protocol.Connection;
import protocol.DisconnectHandler;
import util.PEMParser;
import util.Program;
import util.config.CliParser;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class Main extends Program {

    private InetSocketAddress api_address;
    private InetSocketAddress targetAddress;
    private Connection connection;
    private Context context;
    private RSAPublicKey targetHostkey;
    private boolean listenMode;

    Main() {
        super("tests.onion", "API conformance test case for ONION");
    }

    Logger getLogger() {
        return this.logger;
    }

    @Override
    protected void addParserOptions(CliParser parser) {
        // Add options for the target hostkey and target onion P2P address
        parser.addOption(Option.builder("k")
                .required(true)
                .longOpt("hostkey")
                .desc("Target hostkey")
                .optionalArg(false)
                .argName("FILE")
                .hasArg().build());
        parser.addOption(Option.builder("t")
                .required(true)
                .longOpt("address")
                .desc("Target ONION P2P address")
                .optionalArg(false)
                .argName("IP ADDRESS")
                .hasArg().build());
        parser.addOption(Option.builder("p")
                .required(true)
                .longOpt("address")
                .desc("Target ONION P2P address port")
                .optionalArg(false)
                .argName("PORT")
                .hasArg().build());
        parser.addOption(Option.builder("l")
                .required(false)
                .longOpt("listen")
                .desc("Activate listen mode")
                .optionalArg(true)
                .hasArg(false).build());
    }

    @Override
    protected void parseCommandLine(CommandLine cli, CliParser parser) {
        String filename = parser.getConfigFilename("onion.conf");
        OnionConfiguration config;
        try {
            config = new OnionConfigurationImpl(filename);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read config file");
        }
        api_address = config.getAPIAddress();
        //retrieve the target hostkey
        String target_hostkey = cli.getOptionValue("k");
        String hostname = cli.getOptionValue("t");
        String port = cli.getOptionValue("p");
        targetAddress = new InetSocketAddress(hostname, Integer.parseInt(port));
        try {
            targetHostkey = PEMParser.getPublicKeyFromPEM(new File(
                    target_hostkey));
        } catch (IOException | InvalidKeyException ex) {
            throw new RuntimeException(ex);
        }
        listenMode = cli.hasOption("l");
        if (listenMode) {
            logger.info("Running in listen mode; waiting for incoming tunnel connections");
        }
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
            channel = AsynchronousSocketChannel.open(this.group);
        } catch (IOException ex) {
            throw new RuntimeException();
        }
        channel.connect(api_address, channel, new ConnectCompletion());
    }

    private class ConnectCompletion
            implements CompletionHandler<Void, AsynchronousSocketChannel> {

        private final Random random;

        private ConnectCompletion() {
            this.random = new Random();
        }

        @Override
        public void completed(Void arg0, AsynchronousSocketChannel channel) {
            connection = new Connection(channel, new DisconnectHandler(null) {
                @Override
                protected void handleDisconnect(Object closure) {
                    if (!tests.onion.Main.this.inShutdown()) {
                        logger.log(Level.WARNING, "Connection disconnected");
                        connection = null;
                        shutdown();
                    }
                }
            });
            context = new Context(listenMode,
                    connection, targetHostkey, targetAddress,
                    Main.this.logger);
            context.ready();
        }

        @Override
        public void failed(Throwable arg0, AsynchronousSocketChannel arg1) {
            logger.log(Level.SEVERE, "Cannot connect to Onion API");
            shutdown();
        }
    }

    public static void main(String args[]) throws IOException {
        new Main().start(args);
    }
}
