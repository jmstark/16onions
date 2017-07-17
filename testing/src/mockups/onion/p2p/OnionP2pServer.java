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

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import onion.OnionConfigurationImpl;
import protocol.Connection;
import protocol.ProtocolServer;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class OnionP2pServer extends ProtocolServer<P2pContext> {

    public OnionP2pServer(OnionConfigurationImpl config,
            AsynchronousChannelGroup group) throws IOException {
        super(config.getListenAddress(), group);
    }

    @Override
    protected P2pContext handleNewClient(Connection connection) {
        P2pContext context;
        context = new P2pContext(connection);
        return context;
    }

    @Override
    protected void handleDisconnect(P2pContext context) {

    }
}
