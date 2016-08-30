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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.ShortBufferException;
import onionauth.api.OnionAuthClose;
import onionauth.api.OnionAuthDecrypt;
import onionauth.api.OnionAuthDecryptResp;
import onionauth.api.OnionAuthEncrypt;
import onionauth.api.OnionAuthEncryptResp;
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
class AuthClientContextImpl extends MessageHandler<Void> implements
        AuthClientContext {

    private final Connection connection;
    private final HashMap<Integer, PartialSession> partialSessionMap;
    private final HashMap<Integer, Session> sessionMap;
    private final Logger logger;

    public AuthClientContextImpl(Connection connection) {
        super(null);
        this.logger = Main.LOGGER;
        this.connection = connection;
        this.partialSessionMap = new HashMap(30);
        this.sessionMap = new HashMap(50);
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

    @Override
    public void parseMessage(ByteBuffer buf, Protocol.MessageType type,
            Void closure)
            throws MessageParserException, ProtocolException {
        switch (type) {
            case API_AUTH_SESSION_START: {
                logger.info("Received SESSION_START");
                PartialSession session = newPartialSession();
                OnionAuthSessionHS1 message;
                try {
                    message = new OnionAuthSessionHS1(session.getID(), session.
                            getOurKeyHalf().getBytes());
                } catch (MessageSizeExceededException ex) {
                    throw new RuntimeException("This is a bug; please report");
                }
                connection.sendMsg(message);
                logger.log(Level.INFO,
                        "Created partial session with ID {0}", session.getID());
            }
            break;
            case API_AUTH_SESSION_INCOMING_HS1: {
                OnionAuthSessionIncomingHS1 request;
                Session session;
                Key otherKey;
                OnionAuthSessionHS2 reply;

                logger.info("Received SESSION_INCOMING_HS1");
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
                logger.log(Level.INFO,
                        "Created session with ID {0}", session.getID());
            }
            break;
            case API_AUTH_SESSION_INCOMING_HS2: {
                OnionAuthSessionIncomingHS2 request;
                PartialSession partial;
                Session session;
                int id;

                logger.info("Received SESSION_INCOMING_HS2");
                request = OnionAuthSessionIncomingHS2.parse(buf);
                id = (int) request.getId();
                partial = findPartialSession(id);
                if (null == partial) {
                    throw new ProtocolException(
                            "No session with ID: " + id + " found.");
                }
                removePartialSession(id);
                session = partial.completeSession(new KeyImpl(request.
                        getPayload()));
                registerSession(session);
                logger.log(Level.INFO,
                        "Created session with ID {0}", session.getID());
                // we do not have to send anything
            }
            break;
            case API_AUTH_LAYER_ENCRYPT: {
                OnionAuthEncrypt request;
                OnionAuthEncryptResp reply;
                Session[] sessions;

                request = OnionAuthEncrypt.parse(buf);
                sessions = extractSessions(request);
                logger.log(Level.INFO, "Received LAYER_ENCRYPT with {0} layers",
                        sessions.length);
                // do layer encryption
                // Note: data size increases with every layer as we add IV
                byte[] data = request.getPayload();
                for (Session session : sessions) {
                    data = session.encrypt(data);
                }
                try {
                    reply = new OnionAuthEncryptResp((int) request.getId(),
                            data);
                } catch (MessageSizeExceededException ex) {
                    logger.log(Level.SEVERE,
                            "Encryption resulted in bigger message");
                    //FIXME: We need to have another message code here
                    throw new RuntimeException();
                }
                connection.sendMsg(reply);
            }
            break;
            case API_AUTH_LAYER_DECRYPT: {
                OnionAuthDecrypt request;
                List<Session> sessions;
                OnionAuthDecryptResp reply;

                request = OnionAuthDecrypt.parse(buf);
                sessions = Arrays.asList(extractSessions(request));
                logger.log(Level.INFO, "Received LAYER_DECRYPT with {0} layers",
                        sessions.size());
                byte[] data = request.getPayload();
                //reverse the sessions as we decrypt with the last session first
                Collections.reverse(sessions);
                for (Session session : sessions) {
                    try {
                        data = session.decrypt(data);
                    } catch (ShortBufferException ex) {
                        logger.log(Level.SEVERE, "Decryption failed", ex);
                    }
                }
                try {
                    reply = new OnionAuthDecryptResp(request.getId(), data);
                } catch (MessageSizeExceededException ex) {
                    // shouldn't happen as decryption should reduce the payload size
                    throw new RuntimeException(
                            "This is a bug; please report");
                }
                connection.sendMsg(reply);
            }
            break;
            case API_AUTH_SESSION_CLOSE: {
                OnionAuthClose request;

                request = OnionAuthClose.parse(buf);
                logger.log(Level.INFO,
                        "Received session close for {0}",
                        request.getSessionID());
                Session session = findSession(request.getSessionID());
                if (null == session) {
                    logger.log(Level.WARNING,
                            "Asked to close an non-existing session");
                    return;
                }
                removeSession(session.getID());
                logger.log(Level.INFO, "Session {0} removed", session.
                        getID());
                return;
            }
            // The following are message types we send as replies,
            // so we do not expect to handle them here
            case API_AUTH_SESSION_HS1:
            case API_AUTH_SESSION_HS2:
            case API_AUTH_LAYER_ENCRYPT_RESP:
            case API_AUTH_LAYER_DECRYPT_RESP:
                throw new ProtocolException("Invalid message type sent");
            default:
                throw new RuntimeException("This is a bug, please report");
        }
    }

    private Session[] extractSessions(OnionAuthEncrypt request) throws
            ProtocolException {
        long[] ids = request.getSessions();
        Session[] sessions = new Session[ids.length];
        for (int index = 0; index < sessions.length; index++) {
            Session session = findSession((int) ids[index]);
            if (null == session) {
                throw new ProtocolException(
                        "Unknown session ID given");
            }
            sessions[index] = session;
        }
        return sessions;
    }

}
