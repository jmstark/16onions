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

import protocol.Connection;
import protocol.MessageSizeExceededException;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class PartialSessionHS2Impl extends AbstractPartialSessionImpl {

    public PartialSessionHS2Impl(int id, byte[] payload, Connection connection) {
        super(id, payload, connection);
    }

    /**
     * Create AbstractSession without further interaction
     *
     * @param diffiePayload
     * @return session
     * @throws MessageSizeExceededException; but it is not thrown here
     */
    @Override
    public Session completeSession(byte[] diffiePayload) throws
            MessageSizeExceededException {
        return new SessionImpl(id, connection);
    }
}
