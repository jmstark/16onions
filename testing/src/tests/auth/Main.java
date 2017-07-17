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
package tests.auth;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import auth.OnionAuthConfiguration;
import java.security.InvalidKeyException;
import java.security.interfaces.RSAPublicKey;
import org.apache.commons.cli.CommandLine;
import org.ini4j.ConfigParser;
import protocol.DisconnectHandler;
import util.Program;
import util.config.CliParser;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class Main extends Program {

    private InetSocketAddress apiAddress1;
    private InetSocketAddress apiAddress2;
    private RSAPublicKey hostkey1;
    private RSAPublicKey hostkey2;
    private Context[] contexts;
    private TestController controller;
    static Logger LOGGER;
    private final Semaphore semaphore = new Semaphore(2);

    public Main() {
        super("tests.auth", "API conformance test case for Onion Auth");
        contexts = new Context[2];
    }

    @Override
    protected void parseCommandLine(CommandLine cli, CliParser parser) {
        String filename = parser.getConfigFilename("auth.conf");
        OnionAuthTesterConfiguration config;
        try {
            config = new OnionAuthTesterConfiguration(filename);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Unable to read config file: {0}", ex.
                    getLocalizedMessage());
            Runtime.getRuntime().exit(util.ExitStatus.CONF_ERROR);
            return;
        }
        apiAddress1 = config.getAPIAddress();
        apiAddress2 = config.getAddress("api_address2");
        try {
            hostkey1 = config.getHostkey("hostkey1");
            hostkey2 = config.getHostkey("hostkey2");
        } catch (ConfigParser.NoSectionException
                | ConfigParser.NoOptionException
                | ConfigParser.InterpolationException | IOException
                | InvalidKeyException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    protected void cleanup() {
        for (Context context : contexts) {
            if (null != context) {
                context.shutdown(false);
                context = null;
            }
        }
    }

    @Override
    protected void run() {
        AsynchronousSocketChannel channel1;
        AsynchronousSocketChannel channel2;
        try {
            channel1 = AsynchronousSocketChannel.open(this.group);
            channel2 = AsynchronousSocketChannel.open(this.group);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        try {
            semaphore.acquire(2);
        } catch (InterruptedException ex) {
            LOGGER.warning("Interrupted");
            return;
        }
        channel1.connect(apiAddress1, channel1, new ConnectCompletion(0));
        channel2.connect(apiAddress2, channel2, new ConnectCompletion(1));
        try {
            semaphore.acquire(1);
        } catch (InterruptedException ex) {
            LOGGER.warning("Could not connect to OnionAuth API modules");
            return;
        }
        assert (null == controller);
        controller = new TestController(contexts[0], contexts[1],
                hostkey1, hostkey2,
                scheduledExecutor);
        try {
            controller.start();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Exception in test run");
            ex.printStackTrace();
            shutdown();
        }
        shutdown();
    }

    private class ConnectCompletion
            implements CompletionHandler<Void, AsynchronousSocketChannel> {

        private final int index;//the index referring to the correct context object to set

        private ConnectCompletion(int index) {
            this.index = index;
        }

        @Override
        public void completed(Void none, AsynchronousSocketChannel channel) {
            contexts[index] = new ContextImpl(channel, new DisconnectHandler(
                    null) {
                @Override
                protected void handleDisconnect(Object closure) {
                    if (!Main.this.inShutdown()) {
                        Context context = contexts[index];
                        LOGGER.log(Level.WARNING, "Connection disconnected");
                        context.shutdown(true);
                        contexts[index] = null;
                        shutdown();
                    }
                }
            });
            semaphore.release();
        }

        @Override
        public void failed(Throwable arg0, AsynchronousSocketChannel arg1) {
            semaphore.release();
            logger.log(Level.SEVERE, "Could not connect to Auth API");
            shutdown();
        }
    }

    public static void main(String[] args) throws IOException {
        Main auth = new Main();
        LOGGER = auth.logger;
        auth.start(args);
    }
}
