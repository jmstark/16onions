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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    private static class FutureImpl implements Future {

        public FutureImpl(int id,
                CompletionHandler<IncompleteSession, ? extends Object> handler) {
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean isCancelled() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean isDone() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Object get() throws InterruptedException, ExecutionException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Object get(long arg0, TimeUnit arg1) throws InterruptedException,
                ExecutionException, TimeoutException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    private enum State {
        START_SESSION,
        OTHER
    };
    private State state;

    public ContextImpl(AsynchronousSocketChannel channel,
            DisconnectHandler disconnectHandler) {
        map = new HashMap(1000);
        connection = new Connection(channel, disconnectHandler);
        connection.receive(new AuthMessageHandler());
        state = State.OTHER;
    }

    @Override
    public Future<IncompleteSession> startSession(RSAPublicKey key,
            CompletionHandler<IncompleteSession, ? extends Object> handler) {
        OnionAuthSessionStartMessage message;
        try {
            message = new OnionAuthSessionStartMessage(key);
        } catch (MessageSizeExceededException ex) {
            throw new RuntimeException("Public key too big");
        }
        connection.sendMsg(message);
        state = State.START_SESSION;
        future = new FutureImpl(0, handler);
    }

    @Override
    public Future<Session> deriveSession(RSAPublicKey key, byte[] diffePayload,
            CompletionHandler<Session, ? extends Object> handler) {
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
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

}
