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

import auth.api.OnionAuthDecryptResp;
import auth.api.OnionAuthEncryptResp;
import auth.api.OnionAuthSessionHS1;
import auth.api.OnionAuthSessionHS2;
import auth.api.OnionAuthSessionIncomingHS1;
import auth.api.OnionAuthSessionStartMessage;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    private enum State {
        START_SESSION, //we start a session by sending SESSION_START
        START_SESSION_HS1, //we start a session by sending SESSION_INCOMNIG_HS1
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
    public Future<PartialSession> startSession(RSAPublicKey key,
            CompletionHandler<PartialSession, Void> handler) {
        OnionAuthSessionStartMessage message;
        try {
            message = new OnionAuthSessionStartMessage(RequestID.get(), key);
        } catch (MessageSizeExceededException ex) {
            throw new RuntimeException("Public key too big");
        }
        connection.sendMsg(message);
        state = State.START_SESSION;
        future = new FutureImpl(handler, key);
        return future;
    }

    @Override
    public Future<PartialSession> deriveSession(RSAPublicKey key,
            byte[] diffiePayload,
            CompletionHandler<PartialSession, Void> handler) throws
            MessageSizeExceededException {
        OnionAuthSessionIncomingHS1 message;
        message = new OnionAuthSessionIncomingHS1(RequestID.get(), key,
                diffiePayload);
        connection.sendMsg(message);
        /**
         * we assume that SESSION_START and START_SESSION_HS1 are progressed
         * synchronously. This is because of the limitation of the
         * specification: AUTH_SESSION_HS1 and AUTH_SESSION_HS2 messages have no
         * fields to link them to a response. These messages should also contain
         * the key and payload from their corresponding requests so that the
         * responses can be linked to the requests.
         */
        assert (State.OTHER == state);
        assert (null == future);
        state = State.START_SESSION_HS1;
        future = new FutureImpl(handler, key);
        return future;
    }

    @Override
    public void shutdown(boolean disconnected) {
        if (!disconnected) {
            connection.disconnect();
        }
    }

    @Override
    public Tunnel createTunnel(Session session) {
        return new TunnelImpl(session, connection);
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
                     {
                         OnionAuthSessionHS1 message = null;
                         PartialSessionHS1Impl session;
                         try {
                        message = OnionAuthSessionHS1.parse(buf);
                    } catch (MessageParserException messageParserException) {
                        future.triggerException(
                                new ExecutionException(messageParserException),
                                null);
                    }
                         logger.log(Level.FINER, "Parsed AUTH SESSION HS1");
                    if (null != message) {
                        session = new PartialSessionHS1Impl(message.getSessionID(),
                                message.getPayload(), connection);
                        logger.log(Level.FINEST, "Created IncompleteSession");
                        future.trigger(session, null);
                        }
                    }
                    future = null;
                    state = State.OTHER;
                    return;
                case START_SESSION_HS1:
                    switch (type) {
                        case API_AUTH_SESSION_HS2:
                            break;
                        default:
                            throw new ProtocolException("Expecting HS2 message");
                    }
                    logger.log(Level.FINE, "Received AUTH SESSION HS2 message");
                     {
                         OnionAuthSessionHS2 message = null;
                         PartialSession session;
                         message = OnionAuthSessionHS2.parse(buf);
                         logger.log(Level.FINER, "Parsed AUTH SESSION HS2");
                         session = new PartialSessionHS2Impl(message.
                                 getSessionID(),
                                 message.getPayload(), connection);
                         logger.log(Level.FINEST, "Created ReceiverSession");
                         future.trigger(session, null);
                    }
                    future = null;
                    state = State.OTHER;
                    return;
                default:
                    //encryption and decryption happens here
                    switch (type) {
                        case API_AUTH_LAYER_ENCRYPT_RESP: {
                            OnionAuthEncryptResp message
                                    = OnionAuthEncryptResp.parse(buf);
                            logger.log(Level.FINE,
                                    "Received AUTH LAYER ENCRYPT RESP message");
                            FutureImpl future;
                            try {
                                future = TunnelImpl.getFuture(message.
                                        getRequestID());
                            } catch (NoSuchElementException ex) {
                                logger.warning(
                                        "Received encrypt response for an unknown ID");
                                return;
                            }
                            future.trigger(message.getPayload(), null);
                        }
                            return;
                        case API_AUTH_LAYER_DECRYPT_RESP: {
                            OnionAuthDecryptResp message
                                    = OnionAuthDecryptResp.parse(buf);
                            logger.log(Level.FINE,
                                    "Received AUTH LAYER DECRYPT RESP message");
                            FutureImpl future;
                            try {
                                future = TunnelImpl.getFuture(message.
                                        getRequestID());
                            } catch (NoSuchElementException ex) {
                                logger.warning(
                                        "Received encrypt response for an unknown ID");
                                return;
                            }
                            future.trigger(message.getPayload(), null);
                        }
                            return;
                        default:
                    }
                    break;
            }
            logger.log(Level.SEVERE, "Received unexpected message of type {0}",
                    type.toString());
            throw new ProtocolException("Protocol reached incorrect state");
        }
    }
}
