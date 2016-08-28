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

    public AuthClientContextImpl(Connection connection) {
        MessageHandler handler;
        this.connection = connection;
        this.partialSessionMap = new HashMap(30);
        handler = new AuthApiMessageHandler();
        connection.receive(handler);
    }

    @Override
    public PartialSession findPartialSession(int id) {
        return partialSessionMap.get(id);
    }

    @Override
    public Session findSession(int id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private PartialSession newPartialSession() {
        PartialSession session;
        session = new PartialSessionImpl();
        partialSessionMap.put(session.getID(), session);
        return session;
    }

    @Override
    public Session createSession(Key key) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
