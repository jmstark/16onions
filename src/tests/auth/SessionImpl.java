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

import onionauth.api.OnionAuthClose;
import protocol.Connection;

/**
 * Class for fully instantiated sessions.
 */
public class SessionImpl implements Session {

    protected final long id;
    protected final Connection connection;

    public SessionImpl(long id, Connection connection) {
        super();
        this.id = id;
        this.connection = connection;
    }

    @Override
    public long getID() {
        return this.id;
    }

    @Override
    public void close() {
        OnionAuthClose message;
        message = new OnionAuthClose((int) id);
        connection.sendMsg(message);
    }
}
