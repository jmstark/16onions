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

/**
 * An onion auth session which is not yet initialised. It requires further steps
 * to be fully initialised.
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public interface IncompleteSession extends Session {

    /**
     * Fully instantiate new session from the DH payload received from the other
     * peer
     *
     * @param diffiePayload the DH payload received from other peer
     * @param handler the handler to receive the instantiated session object
     * @return future object
     */
    public Future<Session> completeSession(byte[] diffiePayload,
            CompletionHandler<Session, ? extends Object> handler);

}
