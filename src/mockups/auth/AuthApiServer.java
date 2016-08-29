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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.logging.Level;
import protocol.Connection;
import protocol.ProtocolServer;

/**
 *
 * @author totakura
 */
class AuthApiServer extends ProtocolServer<AuthClientContext> {

    public AuthApiServer(InetSocketAddress apiAddress, AsynchronousChannelGroup group) throws IOException {
        super(apiAddress, group);
    }

    @Override
    protected AuthClientContext handleNewClient(Connection connection) {
        AuthClientContextImpl context = new AuthClientContextImpl(connection);
        connection.receive(context);
        return context;
    }

    @Override
    protected void handleDisconnect(AuthClientContext closure) {
        Main.LOGGER.log(Level.INFO, "Client disconnected");
    }

}
