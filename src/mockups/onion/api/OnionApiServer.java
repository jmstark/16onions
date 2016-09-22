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
package mockups.onion.api;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;
import mockups.onion.Main;
import onion.OnionConfigurationImpl;
import protocol.Connection;
import protocol.ProtocolServer;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class OnionApiServer extends ProtocolServer<APIContextImpl> {

    private final Logger logger;
    private final AsynchronousChannelGroup group;
    private static final ConcurrentLinkedQueue<APIContextImpl> contexts;

    static {
        contexts = new ConcurrentLinkedQueue();
    }

    public OnionApiServer(OnionConfigurationImpl config,
            AsynchronousChannelGroup group) throws IOException {
        super(config.getAPIAddress(), group);
        this.logger = Main.LOGGER;
        this.group = group;
    }

    @Override
    protected APIContextImpl handleNewClient(Connection connection) {
        logger.fine("A new client has connected");
        APIContextImpl context = new APIContextImpl(connection, group);
        return context;
    }

    @Override
    protected void handleDisconnect(APIContextImpl context) {
        context.destroy();
    }
}
