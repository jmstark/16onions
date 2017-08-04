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

import auth.api.OnionAuthApiMessage;
import auth.api.OnionAuthCipherDecrypt;
import auth.api.OnionAuthCipherDecryptResp;
import auth.api.OnionAuthCipherEncrypt;
import auth.api.OnionAuthCipherEncryptResp;
import auth.api.OnionAuthClose;
import auth.api.OnionAuthDecrypt;
import auth.api.OnionAuthDecryptResp;
import auth.api.OnionAuthEncrypt;
import auth.api.OnionAuthEncryptResp;
import auth.api.OnionAuthError;
import auth.api.OnionAuthSessionHS1;
import auth.api.OnionAuthSessionHS2;
import auth.api.OnionAuthSessionIncomingHS1;
import auth.api.OnionAuthSessionIncomingHS2;
import auth.api.OnionAuthSessionStartMessage;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;
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
                OnionAuthSessionStartMessage request;
                request = OnionAuthSessionStartMessage.parse(buf);
                PartialSession session = newPartialSession();
                OnionAuthSessionHS1 message;
                try {
                    message = new OnionAuthSessionHS1(
                            session.getID(),
                            request.getRequestID(),
                            session.getOurKeyHalf().getBytes());
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
                    reply = new OnionAuthSessionHS2(
                            session.getID(),
                            request.getRequestID(),
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
                id = request.getSessionID();
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
                byte[] cipher = null;
                for (Session session : sessions) {
                    try {
                        if (null == cipher) {
                            cipher = session.encrypt(false, data);
                            continue;
                        }
                        cipher = session.encrypt(true, cipher);
                    } catch (IllegalBlockSizeException ex) {
                        throw new RuntimeException();
                    }
                }
                try {
                    reply = new OnionAuthEncryptResp(request.getRequestID(),
                            cipher);
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
                EncryptDecryptBlock block = null;
                //reverse the sessions as we decrypt with the last session first
                Collections.reverse(sessions);
                for (Session session : sessions) {
                    try {
                        block = session.decrypt(data);
                    } catch (ShortBufferException ex) {
                        logger.log(Level.WARNING,
                                "Decryption failed due to illegal block size");
                        throw new ProtocolException(
                                "Decryption failed due to illegal block size");
                    }
                    if (block.isCipher()) {
                        data = block.getPayload();
                    } else
                        break;
                }
                if (block.isCipher())
                    throw new ProtocolException(
                            "Layer decryption did not give result in plaintext");
                try {
                    reply = new OnionAuthDecryptResp(request.getRequestID(),
                            block.getPayload());
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
            case API_AUTH_CIPHER_ENCRYPT: {
                OnionAuthCipherEncrypt request;
                request = OnionAuthCipherEncrypt.parse(buf);
                logger.log(Level.INFO,
                        "Received CIPHER ENCRYPT message for session:{0} "
                        + "with request ID: {1}",
                        new Object[]{request.getSessionID(), request.
                            getRequestID()});
                Session session = findSession(request.getSessionID());
                if (null == session) {
                    throw new ProtocolException("Given session is not known");
                }
                byte[] cipher = null;
                OnionAuthApiMessage reply;
                try {
                    cipher = session.encrypt(request.isCipher(), request.
                            getPayload());
                } catch (IllegalBlockSizeException ex) {
                    reply = new OnionAuthError(request.getRequestID());
                    connection.sendMsg(reply);
                    return;
                }
                try {
                    reply = new OnionAuthCipherEncryptResp(request.
                            getRequestID(),
                            cipher);
                } catch (MessageSizeExceededException ex) {
                    throw new RuntimeException("this is a bug; please report");
                }
                connection.sendMsg(reply);
            }
            break;
            case API_AUTH_CIPHER_DECRYPT: {
                OnionAuthCipherDecrypt request;
                request = OnionAuthCipherDecrypt.parse(buf);

                logger.log(Level.INFO,
                        "Received CIPHER DECRYPT message in session: {0} "
                        + "with requestID: {1}",
                        new Object[]{request.getSessionID(), request.
                            getRequestID()});
                EncryptDecryptBlock block;
                Session session;
                session = findSession(request.getSessionID());
                if (null == session) {
                    throw new ProtocolException("Given session is not known");
                }
                try {
                    block = session.decrypt(request.getPayload());
                } catch (ShortBufferException ex) {
                    throw new ProtocolException(
                            "Asked to decrypt a block with illegal block length");
                }
                OnionAuthCipherDecryptResp reply;
                try {
                reply = new OnionAuthCipherDecryptResp(block.isCipher(),
                        request.getRequestID(), block.getPayload());
                } catch (MessageSizeExceededException ex) {
                    throw new RuntimeException("This is a bug; please report");
                }
                connection.sendMsg(reply);
            }
            break;
            // The following are message types we send as replies,
            // so we do not expect to handle them here
            case API_AUTH_SESSION_HS1:
            case API_AUTH_SESSION_HS2:
            case API_AUTH_LAYER_ENCRYPT_RESP:
            case API_AUTH_LAYER_DECRYPT_RESP:
            case API_AUTH_CIPHER_ENCRYPT_RESP:
            case API_AUTH_CIPHER_DECRYPT_RESP:
                throw new ProtocolException("Invalid message type sent");
            default:
                throw new RuntimeException("This is a bug, please report");
        }
    }

    private Session[] extractSessions(OnionAuthEncrypt request) throws
            ProtocolException {
        int[] ids = request.getSessions();
        Session[] sessions = new Session[ids.length];
        for (int index = 0; index < sessions.length; index++) {
            Session session = findSession((int) ids[index]);
            if (null == session) {
                throw new ProtocolException(
                        "Unknown session ID " + (int) ids[index] + " given");
            }
            sessions[index] = session;
        }
        return sessions;
    }

}
