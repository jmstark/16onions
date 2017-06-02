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
 * MERCHANTABILITY or FITNESS FOR B PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package mockups.onion.p2p;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.security.interfaces.RSAPublicKey;
import protocol.Connection;
import protocol.DisconnectHandler;
import protocol.MessageHandler;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;
import protocol.ProtocolException;

public class P2PServiceImpl implements P2PService {

    @Override
    public <A> void createTunnel(AsynchronousChannelGroup group,
            InetSocketAddress address, RSAPublicKey hostkey,
            TunnelEventHandler<A> handler) throws IOException {
        AsynchronousSocketChannel channel;
        channel = AsynchronousSocketChannel.open(group);
        channel.connect(address, channel,
                new OnionConnectCompletionHandler(address, hostkey, handler));
    }

    /**
     * Connection completion handler to be called when the connection to the
     * other peer's onion has been successfully completed.
     */
    private static class OnionConnectCompletionHandler<A> implements
            CompletionHandler<Void, AsynchronousSocketChannel> {

        private final InetSocketAddress address;
        private final TunnelEventHandler<A> handler;
        private final RSAPublicKey hostkey;

        private OnionConnectCompletionHandler(InetSocketAddress address,
                RSAPublicKey hostkey,
                TunnelEventHandler<A> handler) {
            this.address = address;
            this.handler = handler;
            this.hostkey = hostkey;
        }

        @Override
        public void completed(Void arg0, AsynchronousSocketChannel channel) {
            Connection connection;
            OnionDisconnectHandler disconnectHandler;
            HelloMessage hello;
            disconnectHandler = new OnionDisconnectHandler(handler);
            connection = new Connection(channel, disconnectHandler);
            A context = handler.newContext();
            TunnelImpl<A> tunnel;
            tunnel = new TunnelImpl(context, connection);
            handler.tunnelCreated(tunnel, hostkey);
            disconnectHandler.setTunnel(tunnel);
            try {
                hello = new HelloMessage(hostkey);
            } catch (MessageSizeExceededException ex) {
                handler.tunnelCreatefailed(ex, address, hostkey);
                connection.disconnect();
                return;
            }
            connection.sendMsg(hello);
            connection.receive(new TunnelDataHandler(tunnel, handler));
        }

        @Override
        public void failed(Throwable arg0, AsynchronousSocketChannel channel) {
            handler.tunnelCreatefailed(arg0, address, hostkey);
        }
    }

    private static class OnionDisconnectHandler extends DisconnectHandler<Void> {
        private Tunnel tunnel;
        private final TunnelEventHandler handler;

        public OnionDisconnectHandler(TunnelEventHandler handler) {
            super(null);
            tunnel = null;
            this.handler = handler;
        }

        private void setTunnel(Tunnel tunnel) {
            this.tunnel = tunnel;
        }

        @Override
        protected void handleDisconnect(Void nothing) {
            if (null != tunnel) {
                handler.handleDisconnect(tunnel);
            }
        }
    }

    private static class TunnelDataHandler extends MessageHandler<TunnelEventHandler> {

        private final Tunnel tunnel;

        private TunnelDataHandler(Tunnel tunnel, TunnelEventHandler handler) {
            super(handler);
            this.tunnel = tunnel;
        }

        @Override
        public void parseMessage(ByteBuffer buf, Protocol.MessageType type,
                TunnelEventHandler handler) throws MessageParserException,
                ProtocolException {
            switch (type) {
                case ONION_DATA:
                    DataMessage message;
                    message = DataMessage.parse(buf);
                    handler.handleReceivedData(tunnel, message.getData());
                    return;
                default:
                    throw new ProtocolException("Unknown message type received");
            }
        }

    }

    static final P2PService service = new P2PServiceImpl();
    static RSAPublicKey ourHostkey;

    public static P2PService getInstance(RSAPublicKey hostkey) {
        if (null == ourHostkey) {
            ourHostkey = hostkey;
        }
        return service;
    }

}
