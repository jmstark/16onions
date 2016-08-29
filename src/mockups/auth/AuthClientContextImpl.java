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
package mockups.auth;

import java.nio.ByteBuffer;
import java.util.HashMap;
import onionauth.api.OnionAuthSessionHS1;
import onionauth.api.OnionAuthSessionHS2;
import onionauth.api.OnionAuthSessionIncomingHS1;
import onionauth.api.OnionAuthSessionIncomingHS2;
import protocol.Connection;
import protocol.MessageHandler;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;
import protocol.ProtocolException;

/**
 *
 * @author totakura
 */
class AuthClientContextImpl implements AuthClientContext {

    private final Connection connection;
    private final HashMap<Integer, PartialSession> partialSessionMap;
    private final HashMap<Integer, Session> sessionMap;

    public AuthClientContextImpl(Connection connection) {
        MessageHandler handler;
        this.connection = connection;
        this.partialSessionMap = new HashMap(30);
        this.sessionMap = new HashMap(50);
        handler = new AuthApiMessageHandler();
        connection.receive(handler);
    }

    @Override
    public PartialSession findPartialSession(int id) {
        return partialSessionMap.get(id);
    }

    @Override
    public Session findSession(int id) {
        return sessionMap.get(id);
    }

    @Override
    public PartialSession newPartialSession() {
        PartialSession session;
        session = new PartialSessionImpl();
        partialSessionMap.put(session.getID(), session);
        return session;
    }

    private void removePartialSession(int id) {
        this.partialSessionMap.remove(id);
    }

    @Override
    public Session createSession(Key key) {
        Session session;
        session = new PartialSessionImpl().completeSession(key);
        registerSession(session);
        return session;
    }

    private void registerSession(Session session) {
        sessionMap.put(session.getID(), session);
    }

    private void removeSession(int id) {
        this.sessionMap.remove(id);
    }

    private class AuthApiMessageHandler extends MessageHandler<Void> {

        public AuthApiMessageHandler() {
            super(null);
        }

        @Override
        public void parseMessage(ByteBuffer buf, Protocol.MessageType type,
                Void closure)
                throws MessageParserException, ProtocolException {
            switch (type) {
                case API_AUTH_SESSION_START:
                {
                    PartialSession session = newPartialSession();
                    OnionAuthSessionHS1 message;
                    try {
                        message = new OnionAuthSessionHS1(session.getID(), session.getOurKeyHalf().getBytes());
                    } catch (MessageSizeExceededException ex) {
                        throw new RuntimeException("This is a bug; please report");
                    }
                    connection.sendMsg(message);
                }
                break;
                case API_AUTH_SESSION_INCOMING_HS1: {
                    OnionAuthSessionIncomingHS1 request;
                    Session session;
                    Key otherKey;
                    OnionAuthSessionHS2 reply;

                    request = OnionAuthSessionIncomingHS1.parse(buf);
                    otherKey = new KeyImpl(request.getPayload());
                    session = createSession(otherKey);
                    try {
                    reply = new OnionAuthSessionHS2(session.getID(),
                            session.getOurKeyHalf().getBytes());
                    } catch (MessageSizeExceededException ex) {
                        throw new RuntimeException("This is a bug; please report");
                    }
                    connection.sendMsg(reply);
                }
                break;
                case API_AUTH_SESSION_INCOMING_HS2: {
                    OnionAuthSessionIncomingHS2 request;
                    PartialSession partial;
                    Session session;
                    int id;

                    request = OnionAuthSessionIncomingHS2.parse(buf);
                    id = (int) request.getId();
                    partial = findPartialSession(id);
                    if (null == partial) {
                        throw new ProtocolException("No session with ID: " + id + " found.");
                    }
                    removePartialSession(id);
                    session = partial.completeSession(new KeyImpl(request.getPayload()));
                    registerSession(session);
                    // we do not have to send anything
                }
                break;
                // The following are message types we send a replies, so we do not expect to handle them here
                case API_AUTH_SESSION_HS1:
                case API_AUTH_SESSION_HS2:
                case API_AUTH_LAYER_ENCRYPT_RESP:
                case API_AUTH_LAYER_DECRYPT_RESP:
                    throw new ProtocolException("Invalid message type sent");
                default:
                    throw new RuntimeException("This is a bug, please report");
            }
        }
    }

}
