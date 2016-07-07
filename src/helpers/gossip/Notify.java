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
import gossip.api.NotificationMessage;
import gossip.api.NotifyMessage;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import protocol.Connection;
import protocol.DisconnectHandler;
import protocol.MessageHandler;
import protocol.MessageParserException;
import protocol.Protocol;
import protocol.ProtocolException;
import tools.config.CliParser;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class Notify {

    private static final Logger LOGGER = Logger.getLogger(
            "helpers.gossip.Notify");
    private static final AtomicBoolean inShutdown = new AtomicBoolean();
    private static final int DATATYPE = Publish.DATATYPE;
    private static final SimpleDateFormat DATEFORMAT
            = new SimpleDateFormat("HH:mm:ss");
    private static InetSocketAddress api_address;
    private static AsynchronousChannelGroup group;
    private static Connection connection;

    public static void configure(String[] args) {
        CliParser parser = new CliParser("helpers.gossip.Notify",
                "Notifies whenever a message published through Gossip is seen.");
        parser.parse(args);
        String filename = parser.getConfigFilename("gossip.conf");
        GossipConfiguration config;
        try {
            config = new GossipConfigurationImpl(filename);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read config file");
        }
        api_address = config.getAPIAddress();
    }

    private static void shutdown() {
        inShutdown.set(true);
        if (null != connection) {
            connection.disconnect();
        }
        LOGGER.fine("shutting down...");
        group.shutdown();
    }

    private static void display(String message) {
        Date date = new Date();
        System.out.println(DATEFORMAT.format(date) + ": " + message);
    }

    public static void main(String[] args) throws IOException {
        AsynchronousSocketChannel channel;

        configure(args);
        group = AsynchronousChannelGroup.withFixedThreadPool(1,
                Executors.defaultThreadFactory());
        channel = AsynchronousSocketChannel.open(group);
        channel.connect(api_address, channel, new ConnectHandler());
    }

    private static class ConnectHandler implements
            CompletionHandler<Void, AsynchronousSocketChannel> {

        @Override
        public void completed(Void arg0, AsynchronousSocketChannel channel) {
            connection = new Connection(channel, new DisconnectHandler(null) {
                @Override
                protected void handleDisconnect(Object closure) {
                    LOGGER.log(Level.SEVERE, "API connection disconnected");
                    connection = null;
                    shutdown();
                }
            });
            //send notification
            NotifyMessage notify;
            notify = new NotifyMessage(DATATYPE);
            connection.sendMsg(notify);
            connection.receive(new NotificationHandler(connection));
        }

        @Override
        public void failed(Throwable arg0, AsynchronousSocketChannel arg1) {
            LOGGER.log(Level.SEVERE, "Cannot connect to Gossip API");
            shutdown();
        }
    }

    private static class NotificationHandler
            extends MessageHandler<Connection> {

        public NotificationHandler(Connection connection) {
            super(connection);
        }

        @Override
        public void parseMessage(ByteBuffer buf, Protocol.MessageType type,
                Connection closure) throws MessageParserException,
                ProtocolException {
            if (type != Protocol.MessageType.API_GOSSIP_NOTIFICATION) {
                throw new ProtocolException();
            }
            NotificationMessage notification = NotificationMessage.parse(buf);
            String content = new String(notification.getData(),
                    Charset.forName("UTF-8"));
            LOGGER.log(Level.INFO, "Received: {0}", content);
            display(content);
        }
    }
}
