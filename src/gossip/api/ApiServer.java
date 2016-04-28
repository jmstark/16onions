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
package gossip.api;

import gossip.Cache;
import gossip.api.ApiMessageHandler;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import protocol.Connection;
import protocol.ProtocolServer;

/**
 * Class for handling API requests
 *
 * @author totakura
 */
public class ApiServer extends ProtocolServer<ClientContext> {

    private final Cache cache;

    ApiServer(SocketAddress address,
            AsynchronousChannelGroup group,
            Cache cache) throws IOException {
        super(address, group);
        this.cache = cache;
    }

    @Override
    protected ClientContext handleNewClient(Connection connection) {
        ClientContext context;
        context = new ClientContext(connection);
        connection.receive(new ApiMessageHandler(context, cache));
        return context;
    }

    @Override
    protected void handleDisconnect(ClientContext context) {
        context.close();
    }

}
