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
package mockups.onion.p2p;

import protocol.Connection;
import protocol.MessageSizeExceededException;

class TunnelImpl<A> implements Tunnel<A> {

    private final A context;
    private final Connection connection;

    TunnelImpl(A context, Connection connection) {
        this.context = context;
        this.connection = connection;
    }

    @Override
    public A getContext() {
        return this.context;
    }

    @Override
    public void forwardData(byte[] data) throws MessageSizeExceededException {
        DataMessage message;
        message = new DataMessage(data);
        connection.sendMsg(message);
    }

    @Override
    public void destroy() {
        connection.disconnect();
    }

}
