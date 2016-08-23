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
package tests.auth;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import onionauth.api.OnionAuthClose;
import onionauth.api.OnionAuthSessionHS1;
import onionauth.api.OnionAuthSessionHS2;
import onionauth.api.OnionAuthSessionStartMessage;
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
class ContextImpl implements Context {

    private final Connection connection;
    private final HashMap<Integer, Future> map;
    private FutureImpl future;
    private final Logger logger;

    /**
     * Class for fully instantiated sessions.
     */
    public class SessionImpl implements Session {

        private final long id;

        public SessionImpl(long id) {
            super();
            this.id = id;
        }

        @Override
        public long getID() {
            return this.id;
        }

        @Override
        public void close() {
            closeSession(this.id);
        }
    }

    /**
     * Class for partially initiated sessions. The session requires another DH
     * counterpart to be complete.
     */
    private class IncompleteSessionImpl implements IncompleteSession {

        private final long id;
        private final byte[] payload;

        public IncompleteSessionImpl(long id, byte[] payload) {
            super();
            this.id = id;
            this.payload = payload;
        }

        @Override
        public Session completeSession(byte[] diffiePayload) throws
                MessageSizeExceededException {
            OnionAuthSessionHS2 message;
            message = new OnionAuthSessionHS2(id, diffiePayload);
            connection.sendMsg(message);
            return new SessionImpl(this.id);
        }

        @Override
        public long getID() {
            return this.id;
        }

        @Override
        public void close() {
            closeSession(this.id);
        }
    }

    private void closeSession(long id) {
        OnionAuthClose message;
        message = new OnionAuthClose(id);
        connection.sendMsg(message);
    }

    private enum State {
        START_SESSION,
        OTHER
    };
    private State state;

    public ContextImpl(AsynchronousSocketChannel channel,
            DisconnectHandler disconnectHandler) {
        logger = Main.LOGGER;
        map = new HashMap(1000);
        connection = new Connection(channel, disconnectHandler);
        connection.receive(new AuthMessageHandler());
        state = State.OTHER;
    }

    @Override
    public Future<IncompleteSession> startSession(RSAPublicKey key,
            CompletionHandler<IncompleteSession, Void> handler) {
        OnionAuthSessionStartMessage message;
        try {
            message = new OnionAuthSessionStartMessage(key);
        } catch (MessageSizeExceededException ex) {
            throw new RuntimeException("Public key too big");
        }
        connection.sendMsg(message);
        state = State.START_SESSION;
        future = new FutureImpl(handler);
        return future;
    }

    @Override
    public Future<Session> deriveSession(RSAPublicKey key, byte[] diffePayload,
            CompletionHandler<Session, Void> handler) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void shutdown(boolean disconnected) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private class AuthMessageHandler extends MessageHandler {

        public AuthMessageHandler() {
            super(null);
        }

        @Override
        public void parseMessage(ByteBuffer buf, Protocol.MessageType type,
                Object closure) throws MessageParserException, ProtocolException {
            switch (state) {
                case START_SESSION:
                    switch (type) {
                        case API_AUTH_SESSION_HS1:
                            break;
                        default:
                            throw new ProtocolException("Excepting HS1 message");
                    }
                    logger.log(Level.FINE, "Received AUTH SESSION HS1");
                    OnionAuthSessionHS1 message = null;
                    IncompleteSessionImpl session;
                    try {
                        message = OnionAuthSessionHS1.parse(buf);
                    } catch (MessageParserException messageParserException) {
                        future.triggerException(
                                new ExecutionException(messageParserException),
                                null);
                    }
                    logger.log(Level.FINER, "Received AUTH SESSION HS1");
                    if (null != message) {
                        session = new IncompleteSessionImpl(message.getId(),
                                message.getPayload());
                        logger.log(Level.FINEST, "Created IncompleteSession");
                        future.trigger(session, null);
                    }
                    future = null;
                    state = State.OTHER;
                    return;
                default:
                    break;
            }
        }
    }

}
