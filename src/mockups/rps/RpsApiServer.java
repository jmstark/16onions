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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import protocol.Connection;
import protocol.MessageHandler;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;
import protocol.ProtocolException;
import protocol.ProtocolServer;
import rps.RpsConfiguration;
import rps.api.RpsPeerMessage;

/**
 *
 * @author totakura
 */
public class RpsApiServer extends ProtocolServer<Connection> {

    private final Logger logger;
    private final Set<OnionAddress> peers;

    public RpsApiServer(RpsConfiguration config, Set<OnionAddress> peers, AsynchronousChannelGroup group)
            throws IOException {
        super(config.getAPIAddress(), group);
        this.peers = peers;
        this.logger = Main.LOGGER;
    }

    @Override
    protected Connection handleNewClient(Connection connection) {
        logger.fine("Recevied a new client connection");
        RpsMessageHandler handler = new RpsMessageHandler(connection);
        connection.receive(handler);
        return connection;
    }

    @Override
    protected void handleDisconnect(Connection connection) {
        Main.LOGGER.log(Level.INFO, "Client disconnected");
    }

    private class RpsMessageHandler extends MessageHandler<Connection> {

        private final Random random;

        private RpsMessageHandler(Connection connection) {
            super(connection);
            this.random = new Random();
        }

        @Override
        public void parseMessage(ByteBuffer buf, Protocol.MessageType type,
                Connection connection)
                throws MessageParserException, ProtocolException {
            switch (type) {
                case API_RPS_QUERY: {
                    int index;
                    OnionAddress peer;
                    RpsPeerMessage reply;
                    logger.fine("Received RPS QUERY");
                    if (peers.isEmpty()) // we do not know any peers; ignore
                    {
                        logger.warning("We did not yet learn of any other peers; ignoring query.");
                        return;
                    }
                    index = random.nextInt(peers.size());
                    peer = (OnionAddress) peers.toArray()[index];
                    try {
                        reply = new RpsPeerMessage(peer.getAddress(),
                                peer.getHostKey());
                    } catch (MessageSizeExceededException ex) {
                        throw new RuntimeException("This should not happen; please report this as bug");
                    }
                    connection.sendMsg(reply);
                    logger.fine("Sent RPS PEER");
                    return;
                }
                default:
                    throw new ProtocolException("Unknown message received");
            }
        }
    }
}
