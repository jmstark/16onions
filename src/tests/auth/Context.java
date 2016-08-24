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

import java.nio.channels.CompletionHandler;
import java.security.interfaces.RSAPublicKey;
import java.util.concurrent.Future;
import protocol.MessageSizeExceededException;

/**
 * Context object for managing sessions
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public interface Context {

    /**
     * Create a new session with a peer
     *
     * @param key public key of the other peer
     * @param handler handler to receive the newly created session object
     * @return future object
     */
    public Future<IncompleteSession> startSession(RSAPublicKey key,
            CompletionHandler<IncompleteSession, Void> handler);

    /**
     * Fully instantiate new session from the Diffie-Hellman (DH) payload
     * received from the other peer
     *
     * @param key the public key of the other peer
     * @param diffePayload the DH payload
     * @param handler the handler to receive the instantiated session object
     * @return future object
     * @throws exception when the payload size or the given key are too big to
     * be fit into a single API message
     */
    public Future<ReceiverSession> deriveSession(RSAPublicKey key,
            byte[] diffePayload,
            CompletionHandler<ReceiverSession, Void> handler) throws
            MessageSizeExceededException;

    /**
     * Close this context and all its underlying sessions
     *
     * @param disconnected are we shutting down because the connection has been
     * disconnected?
     */
    public void shutdown(boolean disconnected);
}
