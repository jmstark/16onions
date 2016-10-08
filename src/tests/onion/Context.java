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
package tests.onion;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.interfaces.RSAPublicKey;
import java.util.logging.Level;
import java.util.logging.Logger;
import onion.api.OnionErrorMessage;
import onion.api.OnionTunnelBuildMessage;
import onion.api.OnionTunnelDataMessage;
import onion.api.OnionTunnelIncomingMessage;
import onion.api.OnionTunnelReadyMessage;
import protocol.Connection;
import protocol.MessageHandler;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;
import protocol.ProtocolException;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
class Context extends MessageHandler<Void> {

    private final InetSocketAddress targetAddress;
    private final RSAPublicKey targetHostkey;
    private final Connection connection;
    private final Logger logger;
    private final boolean listenMode;

    enum State {
        CREATE_TUNNEL, TUNNEL_CREATED,
    };

    private State state;

    Context(boolean listenMode, Connection connection, RSAPublicKey targetHostkey,
            InetSocketAddress targetAddress, Logger logger) {
        super(null);
        this.connection = connection;
        this.targetHostkey = targetHostkey;
        this.targetAddress = targetAddress;
        this.logger = logger;
        this.listenMode = listenMode;
    }

    void ready() {
        this.connection.receive(this);
        state = State.CREATE_TUNNEL;
        if (!listenMode) {
            createTunnel();
        }
    }

    private void createTunnel() {
        OnionTunnelBuildMessage onionTunnelBuildMessage;
        try {
            onionTunnelBuildMessage = new OnionTunnelBuildMessage(targetAddress,
                    targetHostkey);
        } catch (MessageSizeExceededException ex) {
            throw new RuntimeException("Message size exceeded");
        }
        connection.sendMsg(onionTunnelBuildMessage);
    }

    /**
     * Send data on the tunnel with the given ID
     *
     * @param id the tunnel ID to send the data on
     */
    private void sendData(long id, String actual) {
        OnionTunnelDataMessage message;
        try {
            message = new OnionTunnelDataMessage(id, actual.getBytes());
        } catch (MessageSizeExceededException ex) {
            throw new RuntimeException();
        }
        connection.sendMsg(message);
    }

    @Override
    public void parseMessage(ByteBuffer buf, Protocol.MessageType type,
            Void closure) throws MessageParserException, ProtocolException {
        switch (type) {
            case API_ONION_TUNNEL_READY:
                if (State.CREATE_TUNNEL != state) {
                    throw new ProtocolException(
                            "We did not ask to create a tunnel");
                }
                 {
                    state = State.TUNNEL_CREATED;
                OnionTunnelReadyMessage ready;
                ready = OnionTunnelReadyMessage.parse(buf);
                long id = ready.getId();
                     sendData(id, "Hello World; it's a classical message");
                }
                return;
            case API_ONION_TUNNEL_INCOMING:
            {
                OnionTunnelIncomingMessage message;
                message = OnionTunnelIncomingMessage.parser(buf);
                byte[] otherHostkeyBytes = message.getKeyEncoding();
                logger.fine("Received a new incoming tunnel connection");
            }
                return;
            case API_ONION_TUNNEL_DATA:
            {
                OnionTunnelDataMessage message;
                message = OnionTunnelDataMessage.parse(buf);
                String actual = new String(message.getData());
                logger.log(Level.FINE, "Received data from Tunnel {0}: {1}",
                        new Object[]{message.getId(), actual});
                sendData(message.getId(), "ACK back to you");
            }
            return;
            case API_ONION_ERROR:
            {
                OnionErrorMessage message;
                message = OnionErrorMessage.parser(buf);
                logger.log(Level.WARNING,
                        "Onion error on tunnel {0}while serving request of type: {1}",
                        new Object[]{message.getId(), message.
                            getRequestType().name()});
            }
                return;
            default:
                throw new ProtocolException(
                        "Received unknown message type " + type.name());
        }
    }

}
