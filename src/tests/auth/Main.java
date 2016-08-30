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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import onionauth.OnionAuthConfiguration;
import org.apache.commons.cli.CommandLine;
import protocol.DisconnectHandler;
import tools.Program;
import tools.config.CliParser;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class Main extends Program {

    private InetSocketAddress apiAddress;
    private Context context;
    private TestController controller;
    static Logger LOGGER;
    private final ReentrantLock lock;
    private final Condition condition;

    public Main() {
        super("tests.auth", "API conformance test case for Onion Auth");
        lock = new ReentrantLock();
        condition = lock.newCondition();
    }

    @Override
    protected void parseCommandLine(CommandLine cli, CliParser parser) {
        String filename = parser.getConfigFilename("auth.conf");
        OnionAuthConfiguration config;
        try {
            config = new OnionAuthConfiguration(filename);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read config file");
        }
        apiAddress = config.getAPIAddress();
    }

    @Override
    protected void cleanup() {
        if (null != context) {
            context.shutdown(false);
            context = null;
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
        channel.connect(apiAddress, channel, new ConnectCompletion());
        if (null == controller) {
            lock.lock();
            try {
                condition.await();
            } catch (InterruptedException ex) {
                return;
            } finally {
                lock.unlock();
            }
        }
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

        @Override
        public void completed(Void arg0, AsynchronousSocketChannel channel) {
            context = new ContextImpl(channel, new DisconnectHandler(null) {
                @Override
                protected void handleDisconnect(Object closure) {
                    if (!Main.this.inShutdown()) {
                        LOGGER.log(Level.WARNING, "Connection disconnected");
                        context.shutdown(true);
                        context = null;
                        shutdown();
                    }
                }
            });
            controller = new TestController(context, scheduledExecutor);
            lock.lock();
            try {
                condition.signal();
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void failed(Throwable arg0, AsynchronousSocketChannel arg1) {
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
