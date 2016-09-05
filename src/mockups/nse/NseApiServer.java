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
package mockups.nse;

import java.io.IOException;
import static java.lang.Math.max;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.Random;
import java.util.logging.Level;
import nse.api.EstimateMessage;
import protocol.Connection;
import protocol.MessageHandler;
import protocol.MessageParserException;
import protocol.Protocol;
import protocol.ProtocolException;
import protocol.ProtocolServer;

/**
 *
 * @author totakura
 */
class NseApiServer extends ProtocolServer<MessageHandler> {

    private final int maxEstimate;
    private final int maxDeviation;

    NseApiServer(NseMockupConfiguration config, AsynchronousChannelGroup group)
            throws IOException {
        super(config.getAPIAddress(), group);
        this.maxEstimate = config.getMockupMaxEstimate();
        this.maxDeviation = config.getMockupMaxDeviation();
    }

    @Override
    protected NseMessageHandler handleNewClient(Connection connection) {
        NseMessageHandler handler = new NseMessageHandler(connection);
        connection.receive(handler);
        return handler;
    }

    @Override
    protected void handleDisconnect(MessageHandler closure) {
        Main.LOGGER.log(Level.INFO, "Client disconnected");
    }

    private class NseMessageHandler extends MessageHandler<Connection> {

        private final Random random;

        private NseMessageHandler(Connection connection) {
            super(connection);
            random = new Random();
        }

        @Override
        public void parseMessage(ByteBuffer buf, Protocol.MessageType type,
                Connection connection) throws MessageParserException,
                ProtocolException {
            switch (type) {
                case API_NSE_QUERY:
                    break;
                default:
                    throw new ProtocolException("Unknown message with type: "
                            + type.toString() + " received.");
            }
            EstimateMessage reply;
            reply = new EstimateMessage(random.nextInt(maxEstimate),
                    max(1, random.nextInt(maxDeviation)));
            connection.sendMsg(reply);
        }

    }

}
