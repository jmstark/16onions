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
package mockups.onion.p2p;

import java.nio.ByteBuffer;
import java.security.interfaces.RSAPublicKey;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import mockups.onion.api.OnionApiServer;
import protocol.Connection;
import protocol.MessageHandler;
import protocol.MessageParserException;
import protocol.Protocol;
import protocol.ProtocolException;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
class P2pContext {

    private final List<SimpleImmutableEntry<TunnelEventHandler, Tunnel>> mappings;
    private final Connection connection;

    private enum State {
        STATE_NEW,
        STATE_VERIFIED;
    };
    private State state;

    public P2pContext(Connection connection) {
        this.connection = connection;
        mappings = new LinkedList();
        this.state = State.STATE_NEW;

        connection.receive(new ConnectionReader());
    }

    private <A, B> void notifyNewTunnel(RSAPublicKey key) {
        Iterator<TunnelEventHandler> iterator = OnionApiServer.getAllHandlers();
        while (iterator.hasNext()) {
            TunnelEventHandler<A, B> handler = iterator.next();
            A context = handler.newContext();
            IncomingTunnel<A> tunnel;
            tunnel = new IncomingTunnel<>(context,
                    connection,
                    new TunnelDestroyHandler(handler));
            handler.newIncomingTunnel(tunnel, key);
            SimpleImmutableEntry entry = new SimpleImmutableEntry(handler,
                    tunnel);
            mappings.add(entry);
        }
    }

    private class TunnelDestroyHandler implements IncomingTunnelDestroyHandler {

        private final TunnelEventHandler handler;

        private TunnelDestroyHandler(TunnelEventHandler handler) {
            this.handler = handler;
        }

        @Override
        public void incomingTunnelDestroyed(Tunnel tunnel) {
            SimpleImmutableEntry entry = new SimpleImmutableEntry(handler,
                    tunnel);
            mappings.remove(entry);
            if (0 == mappings.size()) {
                connection.disconnect();
            }
        }
    }

    private class ConnectionReader extends MessageHandler<Void> {

        private ConnectionReader() {
            super(null);
        }

        @Override
        public void parseMessage(ByteBuffer buf, Protocol.MessageType type,
                Void closure) throws MessageParserException, ProtocolException {
            switch (type) {
                case ONION_HELLO:
                    //FIXME: parse HELLO and change state to verify then tripper new tunnel
                    return;
                case ONION_DATA:
                    DataMessage message = DataMessage.parse(buf);
                    for (SimpleImmutableEntry<TunnelEventHandler, Tunnel> entry : mappings) {
                        TunnelEventHandler handler = entry.getKey();
                        Tunnel tunnel = entry.getValue();
                        handler.handleReceivedData(tunnel, message.getData());
                    }
                    return;
                default:
                    throw new ProtocolException("Received unknown data message");
            }
        }
    }
}
