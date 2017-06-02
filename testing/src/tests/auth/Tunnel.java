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
import java.util.concurrent.Future;
import protocol.MessageSizeExceededException;

/**
 * A tunnel formed by chaining multiple sessions.
 *
 * Session chaining is done by addHop and removeHop methods.
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public interface Tunnel {

    /**
     * Chain a session to the end of the tunnel.
     *
     * @param session
     */
    public void addHop(Session session);

    /**
     * Remove a session from the tunnel.
     *
     * The position of the session could be anywhere in the tunnel.
     *
     * @param session
     */
    public boolean removeHop(Session session);

    /**
     * Layer encrypt the given payload.
     *
     * @param payload the data to be encrypted with the chained sessions
     * @param handler completion handler which receives the encrypted payload
     * @return future object
     * @throws protocol.MessageSizeExceededException
     */
    public Future<byte[]> encrypt(byte[] payload,
            CompletionHandler<byte[], ? extends Object> handler) throws
            MessageSizeExceededException;

    /**
     * Layer decrypt the given payload.
     *
     * @param payload the data to be decrypted with chained sessions.
     * @param handler completion handler which receives the decrypted payload
     * @return future object
     * @throws protocol.MessageSizeExceededException
     */
    public Future<byte[]> decrypt(byte[] payload,
            CompletionHandler<byte[], ? extends Object> handler) throws
            MessageSizeExceededException;
}
