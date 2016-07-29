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
import gossip.api.ValidationMessage;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import org.apache.commons.cli.CommandLine;
import protocol.Connection;
import protocol.DisconnectHandler;
import protocol.MessageHandler;
import protocol.MessageParserException;
import protocol.Protocol;
import protocol.ProtocolException;
import tools.Program;
import tools.config.CliParser;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public final class Notify extends Program {

    private static final InputStreamReader INPUT_READER
            = new InputStreamReader(System.in);
    private static final int DATATYPE = Publish.DATATYPE;
    private static final SimpleDateFormat DATEFORMAT
            = new SimpleDateFormat("HH:mm:ss");
    private static InetSocketAddress api_address;
    private static Connection connection;

    public Notify() {
        super("helpers.gossip.Notify",
                "Notifies whenever a message published through Gossip is seen.");
    }

    private static void display(String message) {
        Date date = new Date();
        System.out.println(DATEFORMAT.format(date) + ": " + message);
    }

    private static boolean promptUser() throws IOException {
        int resp;

        while (true) {
            //flush the stream
            while (INPUT_READER.ready()) {
                INPUT_READER.skip(1);
            }
            System.out.println("Does the above message make sense? [Y/N]");
            resp = INPUT_READER.read();
            switch (resp) {
                case 'Y':
                case 'y':
                    return true;
                case 'N':
                case 'n':
                    return false;
                default:
                    System.out.println("Be serious.");
            }
        }

    }

    public static void main(String[] args) throws IOException {
        new Notify().start(args);
    }

    @Override
    protected void parseCommandLine(CommandLine cli, CliParser parser) {
        String filename = parser.getConfigFilename("gossip.conf");
        GossipConfiguration config;
        try {
            config = new GossipConfigurationImpl(filename);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read config file");
        }
        api_address = config.getAPIAddress();
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

    private class NotificationHandler
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
            ValidationMessage validation;
            try {
                validation = new ValidationMessage(notification.getMsgId(),
                        promptUser());
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE,
                        "Unable to handle input stream: {0}",
                        ex);
                shutdown();
                return;
            }
            LOGGER.log(Level.FINE,
                    "Sending validation message for {0}",
                    notification.getMsgId());
            connection.sendMsg(validation);
        }
    }
}
