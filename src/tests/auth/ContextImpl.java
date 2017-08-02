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
    private final HashMap<Long, FutureImpl> map;
    private final Logger logger;

    public ContextImpl(AsynchronousSocketChannel channel,
            DisconnectHandler disconnectHandler) {
        logger = Main.LOGGER;
        map = new HashMap(1000);
        connection = new Connection(channel, disconnectHandler);
        connection.receive(new AuthMessageHandler());
    }

    private class FutureNotFoundException extends Exception {
    };

    /**
     * Look up for the future corresponding to the given request ID
     *
     * @param id the request ID
     * @return the found future; null if nothing is found
     */
    FutureImpl findFuture(long id) throws FutureNotFoundException {
        FutureImpl future = null;
        if (null == future) {
            throw new FutureNotFoundException();
        }
        return future;
    }

    /**
     * Remove future from the map
     *
     * @param id the id of the future to remove
     */
    void removeFuture(long id) {

    }

    /**
     * Add the future into the map by assigning it to the given ID
     *
     * @param id the request ID to which the given future should be associated
     *          with
     * @param future the future
     */
    void addFuture(long id, FutureImpl future) {

    }

    @Override
    public Future<PartialSession> startSession(RSAPublicKey key,
            CompletionHandler<PartialSession, Void> handler) {
        OnionAuthSessionStartMessage message;
        FutureImpl future;
        try {
            message = new OnionAuthSessionStartMessage(RequestID.get(), key);
        } catch (MessageSizeExceededException ex) {
            throw new RuntimeException("Public key too big");
        }
        future = new FutureImpl(handler, key);
        addFuture(message.getRequestID(), future);
        connection.sendMsg(message);
        return future;
    }

    @Override
    public Future<PartialSession> deriveSession(RSAPublicKey key,
            byte[] diffiePayload,
            CompletionHandler<PartialSession, Void> handler) throws
            MessageSizeExceededException {
        OnionAuthSessionIncomingHS1 message;
        FutureImpl future;
        message = new OnionAuthSessionIncomingHS1(RequestID.get(), key,
                diffiePayload);
        future = new FutureImpl(handler, key);
        addFuture(message.getRequestID(), future);
        connection.sendMsg(message);
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

    private void handleSessionHS1(ByteBuffer buf)
            throws MessageParserException, ProtocolException, FutureNotFoundException {
        FutureImpl future;
        logger.log(Level.FINE, "Received AUTH SESSION HS1");

        OnionAuthSessionHS1 message = null;
        PartialSessionHS1Impl session;
        message = OnionAuthSessionHS1.parse(buf);
        logger.log(Level.FINER, "Parsed AUTH SESSION HS1");
        future = findFuture(message.getRequestID());
        session = new PartialSessionHS1Impl(message.getSessionID(),
                message.getPayload(), connection);
        logger.log(Level.FINER,
                "Created a partial session with ID: {0}",
                message.getSessionID());
        future.trigger(session, null);
    }

    private void handleSessionHS2(ByteBuffer buf)
            throws MessageParserException, ProtocolException, FutureNotFoundException {
        FutureImpl future;
        logger.log(Level.FINE, "Received AUTH SESSION HS2 message");
        OnionAuthSessionHS2 message = null;
        PartialSession session;
        message = OnionAuthSessionHS2.parse(buf);
        logger.log(Level.FINER, "Parsed AUTH SESSION HS2");
        future = findFuture(message.getRequestID());
        session = new PartialSessionHS2Impl(message.
                getSessionID(),
                message.getPayload(), connection);
        logger.log(Level.FINEST, "Created ReceiverSession");
        future.trigger(session, null);
    }

    private void handleEncryptResp(ByteBuffer buf)
            throws MessageParserException, ProtocolException, FutureNotFoundException {
        OnionAuthEncryptResp message = OnionAuthEncryptResp.parse(buf);
        logger.log(Level.FINE, "Received AUTH LAYER ENCRYPT RESP message");
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

    private void handleDecryptResp(ByteBuffer buf)
            throws MessageParserException, ProtocolException, FutureNotFoundException {
        OnionAuthDecryptResp message = OnionAuthDecryptResp.parse(buf);
        logger.log(Level.FINE, "Received AUTH LAYER DECRYPT RESP message");
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

    private class AuthMessageHandler extends MessageHandler {

        public AuthMessageHandler() {
            super(null);
        }

        @Override
        public void parseMessage(ByteBuffer buf, Protocol.MessageType type,
                Object closure) throws MessageParserException, ProtocolException {
            try {
                switch (type) {
            case API_AUTH_SESSION_HS1:
                handleSessionHS1(buf);
                break;
            case API_AUTH_SESSION_HS2:
                handleSessionHS2(buf);
                break;
            case API_AUTH_LAYER_ENCRYPT_RESP:
                handleEncryptResp(buf);
            break;
            case API_AUTH_LAYER_DECRYPT_RESP:
                handleDecryptResp(buf);
            break;
            default:
                logger.log(Level.SEVERE, "Received unexpected message of type {0}",
                        type.toString());
                        throw new ProtocolException("Protocol reached incorrect state");
                }
            } catch (FutureNotFoundException ex) {
                logger.warning("Received a message with an unknown request ID; ignoring");
            }
    }

}
}
