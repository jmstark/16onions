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
package mockups.onion.api;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import mockups.onion.Main;
import mockups.onion.p2p.P2PService;
import mockups.onion.p2p.P2PServiceImpl;
import mockups.onion.p2p.Tunnel;
import mockups.onion.p2p.TunnelEventHandler;
import onion.api.OnionCoverMessage;
import onion.api.OnionTunnelBuildMessage;
import onion.api.OnionTunnelDataMessage;
import onion.api.OnionTunnelDestroyMessage;
import onion.api.OnionTunnelIncomingMessage;
import onion.api.OnionTunnelReadyMessage;
import protocol.Connection;
import protocol.MessageHandler;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;
import protocol.ProtocolException;
import util.SecurityHelper;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
class APIContextImpl extends MessageHandler<Void> implements APIContext,
        TunnelEventHandler<Integer> {

    private final Connection connection;
    private final AsynchronousChannelGroup group;
    private final AtomicInteger tunnelID;
    private final Logger logger;
    private final Map<Integer, Tunnel> tunnelMap;
    private final RSAPublicKey hostkey;

    APIContextImpl(RSAPublicKey hostkey, Connection connection,
            AsynchronousChannelGroup group) {
        super(null);
        this.connection = connection;
        this.group = group;
        this.tunnelID = new AtomicInteger(0);
        this.logger = Main.LOGGER;
        this.tunnelMap = new HashMap(20);
        this.hostkey = hostkey;
    }

    private void onionConnect(InetSocketAddress address,
            RSAPublicKey destHostkey) throws IOException {
        P2PService p2p;
        p2p = P2PServiceImpl.getInstance(hostkey);
        p2p.createTunnel(group, address, destHostkey, this);
    }

    @Override
    public Integer newContext() {
        return tunnelID.getAndIncrement();
    }

    @Override
    public void tunnelCreated(Tunnel<Integer> tunnel, RSAPublicKey key) {
        OnionTunnelReadyMessage reply;
        try {
            reply = new OnionTunnelReadyMessage(tunnel.getContext(),
                    SecurityHelper.encodeRSAPublicKey(key));
        } catch (MessageSizeExceededException ex) {
            tunnelID.getAndDecrement();
            logger.warning("This is a bug; please report");
            return;
        }
        synchronized (tunnelMap) {
            tunnelMap.put(tunnel.getContext(), tunnel);
        }
        connection.sendMsg(reply);
    }

    @Override
    synchronized public void tunnelCreatefailed(Throwable exc,
            InetSocketAddress address,
            RSAPublicKey key) {
        logger.log(Level.WARNING,
                "Could not connect to onion module on {0}", address);
        logger.warning("Ignoring TUNNEL BUILD request");
    }

    @Override
    public void newIncomingTunnel(Tunnel<Integer> tunnel) {
        OnionTunnelIncomingMessage message;
        message = new OnionTunnelIncomingMessage(tunnel.getContext());
        synchronized (connection) {
            connection.sendMsg(message);
        }
        synchronized (tunnelMap) {
            tunnelMap.put(tunnel.getContext(), tunnel);
        }
    }

    @Override
    public void handleReceivedData(Tunnel<Integer> tunnel, byte[] data) {
        String msg = new String(data);
        logger.log(Level.FINE, "Sending data \"{0}\" on API through tunnel with ID {1}",
                new Object[]{msg, tunnel.getContext()});
        OnionTunnelDataMessage message;
        try {
            message = new OnionTunnelDataMessage(tunnel.getContext(), data);
        } catch (MessageSizeExceededException ex) {
            throw new RuntimeException("This is a bug; please report");
        }
        synchronized (connection) {
            connection.sendMsg(message);
        }
    }

    @Override
    synchronized public void handleDisconnect(Tunnel<Integer> tunnel) {
        int id = tunnel.getContext();
        logger.log(Level.WARNING, "Tunnel {0} broken.", id);
        tunnelMap.remove(id);
    }

    @Override
    public void destroy() {
        for (Tunnel tunnel : tunnelMap.values()) {
            tunnel.destroy();
        }
    }

    @Override
    public void removeTunnel(int id) {
        logger.log(Level.FINE, "Removing tunnel {0}", id);
        tunnelMap.remove(id);
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
                logger.fine("Received TUNNEL BUILD");
                try {
                    onionConnect(address, request.getKey());
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
                Tunnel tunnel;
                request = OnionTunnelDataMessage.parse(buf);
                id = request.getId();
                tunnel = tunnelMap.get((int) id);
                try {
                tunnel.forwardData(request.getData());
                } catch (MessageSizeExceededException ex) {
                    throw new RuntimeException("This is a bug; please report.");
                }
                logger.log(Level.FINER, "Sent a DATA message on tunnel {0}", id);
                return;
            }
            case API_ONION_TUNNEL_DESTROY: {
                OnionTunnelDestroyMessage request;
                Tunnel tunnel;
                request = OnionTunnelDestroyMessage.parse(buf);
                long id = request.getId();
                /**
                 * First try to remove from the tunnelMap. If the tunnel ID is
                 * not present in tunnel Map, see if it is present in the
                 * incomingMap and remove it from there
                 */
                tunnel = tunnelMap.remove((int) id);
                logger.fine("Received TUNNEL DESTROY");
                if (null == tunnel) {
                    logger.log(Level.WARNING,
                            "Asked to destroy an unknown tunnel {0}", id);
                    return;
                }
                tunnel.destroy();
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
