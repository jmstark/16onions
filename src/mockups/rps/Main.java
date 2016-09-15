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
package mockups.rps;

import gossip.api.NotificationMessage;
import gossip.api.NotifyMessage;
import gossip.api.ValidationMessage;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.security.InvalidKeyException;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import protocol.Connection;
import protocol.DisconnectHandler;
import protocol.MessageHandler;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;
import protocol.ProtocolException;
import protocol.ProtocolServer;
import rps.RpsConfiguration;
import rps.RpsConfigurationImpl;
import tests.auth.Context;
import util.Program;
import util.config.CliParser;

/**
 *
 * @author totakura
 */
public class Main extends Program {
    static Logger LOGGER;
    private InetSocketAddress apiAddress;
    private Context context;
    private ProtocolServer server;
    private RpsConfiguration config;
    private Connection gossipConnection;
    private static boolean success;
    private ScheduledFuture<?> publishFuture;
    private final LinkedHashSet peers;

    public Main() {
        super("mockups.rps", "Mockup module for RPS");
        this.peers = new LinkedHashSet(300);
    }

    @Override
    protected void parseCommandLine(CommandLine cli, CliParser parser) {
        String filename = parser.getConfigFilename("rps.conf");

        try {
            config = new RpsConfigurationImpl(filename);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read config file: " + ex.
                    getMessage());
        }
    }

    @Override
    protected void cleanup() {
        try {
            server.stop();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        if (null != publishFuture) {
            publishFuture.cancel(false);
        }
    }

    @Override
    protected void run() {
        /* start gossip client; use code from gossip helpers */
        AsynchronousSocketChannel channel;
        try {
            channel = AsynchronousSocketChannel.open(group);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        channel.connect(config.getGossipAPIAddress(),
                channel, new ConnectHandler());
        try {
            /**
             * then start the api server; api server should have a context and
             * the context should have functions to add new peers learnt from
             * Gossip to a list
             */
            server = new RpsApiServer(config, peers, group);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE,
                    "Could not start the API server due to {0}.  Cannot continue.",
                    ex.getCause());;
            shutdown();
        }
    }

    /**
     * Contruct the publish message and give it to gossip.
     */
    private void publish() {
        MembershipMessage message;
        try {
            message = new MembershipMessage(config.getHostKey(),
                    config.getOnionP2PAddress());
        } catch (InvalidKeyException | NoSuchElementException | IOException ex) {
            throw new RuntimeException("Hostkey not found or could not be read");
        }
        try {
            gossipConnection.sendMsg(message.encapsulateAsAnnounce());
        } catch (MessageSizeExceededException ex) {
            throw new RuntimeException("This should not happen; please report this as bug");
        }
    }

    private void schedulePublish() {
        publishFuture = scheduledExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if ((null != gossipConnection) && gossipConnection.getChannel().isOpen()) {
                    publish();
                }
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    private class ConnectHandler implements
            CompletionHandler<Void, AsynchronousSocketChannel> {

        @Override
        public void completed(Void arg0, AsynchronousSocketChannel channel) {
            NotifyMessage notify;
            gossipConnection = new Connection(channel, new DisconnectHandler(null) {
                @Override
                protected void handleDisconnect(Object closure) {
                    if (!success) {
                        logger.log(Level.SEVERE, "Publishing failed");
                        shutdown();
                    }
                }
            });
            notify = new NotifyMessage(MembershipMessage.DATATYPE);
            gossipConnection.sendMsg(notify);
            gossipConnection.receive(new GossipNotificationHandler(peers,
                    gossipConnection));
            publish();
            schedulePublish();
        }

        @Override
        public void failed(Throwable arg0, AsynchronousSocketChannel arg1) {
            logger.log(Level.SEVERE, "Cannot connect to Gossip API");
            shutdown();
        }
    }

    private static class GossipNotificationHandler extends MessageHandler<Connection> {

        private final Set<OnionAddress> set;

        public GossipNotificationHandler(Set<OnionAddress> set, Connection connection) {
            super(connection);
            this.set = set;
        }

        @Override
        public void parseMessage(ByteBuffer buf, Protocol.MessageType type,
                Connection connection)
                throws MessageParserException, ProtocolException {
            switch (type) {
                case API_GOSSIP_NOTIFICATION:
                    /**
                     * Parse notification and add the peer to the set. Send a
                     * validation message signalling to Gossip if the
                     * notification message is valid or not.
                     */
                    NotificationMessage notification;
                    MembershipMessage membershipMessage;
                    OnionAddress onion;
                    ValidationMessage validation;
                    notification = NotificationMessage.parse(buf);
                    membershipMessage = MembershipMessage.parseFromNotification(notification);
                    onion = null;
                    try {
                        onion = new OnionAddress(membershipMessage.getAddress(),
                                membershipMessage.getKeyEncoding());
                    } catch (InvalidKeyException ex) {
                        LOGGER.warning("Received a malformed RPS membership message");
                    }
                    if (null != onion) {
                        set.add(onion);
                    }
                    validation = new ValidationMessage(notification.getMsgId(),
                            null != onion);
                    connection.sendMsg(validation);
                    return;
                default:
                    throw new ProtocolException("Unexpected message received");
            }
        }

    }

    public static void main(String[] args) throws IOException {
        Main mockup = new Main();
        LOGGER = mockup.logger;
        mockup.start(args);
    }
}
