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
import java.util.logging.Level;
import java.util.logging.Logger;
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

    public Main() {
        super("tests.auth", "API conformance test case for Onion Auth");
    }

    @Override
    protected void parseCommandLine(CommandLine cli, CliParser parser) {
        String filename = parser.getConfigFilename("auth.conf");
        AuthConfiguration config;
        try {
            config = new AuthConfiguration(filename);
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
        }

        @Override
        public void failed(Throwable arg0, AsynchronousSocketChannel arg1) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }
}
