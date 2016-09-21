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
package mockups.onion;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import onion.api.OnionCoverMessage;
import onion.api.OnionErrorMessage;
import onion.api.OnionTunnelBuildMessage;
import onion.api.OnionTunnelDataMessage;
import onion.api.OnionTunnelDestroyMessage;
import onion.api.OnionTunnelReadyMessage;
import protocol.Connection;
import protocol.DisconnectHandler;
import protocol.MessageHandler;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;
import protocol.ProtocolException;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
class ClientContext extends MessageHandler<Void> {

    private final Connection connection;
    private final AsynchronousChannelGroup group;
    private AtomicInteger tunnelID;
    private final Logger logger;
    private final Map<Integer, Connection> tunnelMap;

    ClientContext(Connection connection, AsynchronousChannelGroup group) {
        super(null);
        this.connection = connection;
        this.group = group;
        this.tunnelID = new AtomicInteger(0);
        this.logger = Main.LOGGER;
        this.tunnelMap = new HashMap(20);
    }

    private void onionConnect(InetSocketAddress address, byte[] hostkey) throws
            IOException {
        AsynchronousSocketChannel channel;
        channel = AsynchronousSocketChannel.open(group);
        channel.connect(address, channel,
                new OnionConnectCompletionHandler(address, hostkey));
    }

    void destroy() {
        for (Connection peerConnection : tunnelMap.values()) {
            peerConnection.disconnect();
        }
    }

    private class OnionDisconnectHandler extends DisconnectHandler<Integer> {

        public OnionDisconnectHandler(int id) {
            super(id);
        }

        /**
         * The P2P Onion connection to the other peer is broken and now the
         * tunnel is invalid.
         *
         * We remove the tunnel from the map so that any future requests on
         * using this tunnel will be replied with an error message
         *
         * @param id the tunnel id
         */
        @Override
        protected void handleDisconnect(Integer id) {
            logger.warning("Tunnel " + id + " broken.");
            tunnelMap.remove(id);
        }
    }

    private class OnionConnectCompletionHandler implements
            CompletionHandler<Void, AsynchronousSocketChannel> {

        private final byte[] hostkey;
        private final InetSocketAddress address;

        public OnionConnectCompletionHandler(InetSocketAddress address,
                byte[] hostkey) {
            this.hostkey = hostkey;
            this.address = address;
        }

        @Override
        public void completed(Void arg0, AsynchronousSocketChannel channel) {
            OnionTunnelReadyMessage reply;
            int id = tunnelID.getAndIncrement();
            try {
                reply = new OnionTunnelReadyMessage(id, hostkey);
            } catch (MessageSizeExceededException ex) {
                tunnelID.getAndDecrement();
                logger.warning("This is a bug; please report");
                return;
            }
            // Create the peer connection and add it to our map
            Connection peerConnection;
            peerConnection = new Connection(channel, new OnionDisconnectHandler(
                    id));
            tunnelMap.put(id, peerConnection);
            connection.sendMsg(reply);
        }

        @Override
        public void failed(Throwable arg0, AsynchronousSocketChannel channel) {
            logger.log(Level.WARNING,
                    "Could not connect to onion module on {0}", address);
            logger.warning("Ignoring TUNNEL BUILD request");
        }
    }

    @Override
    public void parseMessage(ByteBuffer buf, Protocol.MessageType type,
            Void closure) throws MessageParserException, ProtocolException {
        switch (type) {
            case API_ONION_TUNNEL_BUILD: {
                OnionTunnelBuildMessage request;
                InetSocketAddress address;
                request = OnionTunnelBuildMessage.parse(buf);
                address = request.getAddress();
                try {
                    onionConnect(address, request.getEncoding());
                } catch (IOException ex) {
                    logger.log(Level.WARNING,
                            "Unable to open P2P Onion connection to {0}; ignoring request",
                            address);
                }
                return;
            }
            case API_ONION_TUNNEL_DATA: {
                OnionTunnelDataMessage request;
                long id;
                Connection peerConnection;
                request = OnionTunnelDataMessage.parse(buf);
                id = request.getId();
                peerConnection = tunnelMap.get((int) id);
                peerConnection.sendMsg(request);
                logger.log(Level.FINER, "Sent a DATA message on tunnel {0}", id);
                return;
            }
            case API_ONION_TUNNEL_DESTROY: {
                OnionTunnelDestroyMessage request;
                request = OnionTunnelDestroyMessage.parse(buf);
                long id = request.getId();
                Object present = tunnelMap.remove((int) id);
                if (null == present) {
                    logger.log(Level.WARNING,
                            "Asked to destroy an unknown tunnel {0}", id);
                }
                return;
            }
            case API_ONION_COVER: {
                OnionCoverMessage request;
                request = OnionCoverMessage.parse(buf);
                logger.fine("Received COVER message with cover size " + request.
                        getCoverSize());
                return;
            }
            default:
                throw new ProtocolException("Received unknown request");
        }
    }

}
