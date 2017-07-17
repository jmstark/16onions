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

import protocol.MessageSizeExceededException;

/**
 * Interface to identify a session on the receiver of DH
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public interface PartialSession extends Session {

    /**
     * Return the Diffie-Hellman payload of this session
     *
     * @return the DH payload
     */
    public byte[] getDiffiePayload();

    /**
     * Fully instantiate new session from the DH payload received from the other
     * peer
     *
     * @param diffiePayload the DH payload received from other peer
     * @return the instantiated session object
     * @throws MessageSizeExceededException if the given payload is too big to
     * fit into a single API message
     */
    public Session completeSession(byte[] diffiePayload) throws
            MessageSizeExceededException;
}
