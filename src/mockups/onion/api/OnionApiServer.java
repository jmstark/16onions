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
import java.security.InvalidKeyException;
import java.security.interfaces.RSAPublicKey;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import mockups.onion.Main;
import mockups.onion.p2p.TunnelEventHandler;
import onion.OnionConfiguration;
import protocol.Connection;
import protocol.ProtocolServer;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class OnionApiServer extends ProtocolServer<APIContextImpl> {

    private final Logger logger;
    private final AsynchronousChannelGroup group;
    private static final List<TunnelEventHandler> contexts = new LinkedList();
    private final RSAPublicKey hostkey;

    /**
     * Create the onion API server
     *
     * @param config the configuration
     * @param group the async group to be a part of
     * @throws IOException When unable to create the server
     * @throws NoSuchElementException when hostkey is not found in config
     * @throws InvalidKeyException when hostkey is not found in config
     */
    public OnionApiServer(OnionConfiguration config,
            AsynchronousChannelGroup group) throws IOException,
            NoSuchElementException, InvalidKeyException {
        super(config.getAPIAddress(), group);
        this.logger = Main.LOGGER;
        this.group = group;
        this.hostkey = config.getHostKey();
        logger.log(Level.INFO, "ONION API running on {0}", config.getAPIAddress());
    }

    @Override
    protected APIContextImpl handleNewClient(Connection connection) {
        logger.fine("A new client has connected");
        APIContextImpl context = new APIContextImpl(hostkey, connection, group);
        contexts.add(context);
        connection.receive(context);
        return context;
    }

    @Override
    protected void handleDisconnect(APIContextImpl context) {
        context.destroy();
    }

    public static Iterator<TunnelEventHandler> getAllHandlers() {
        return contexts.iterator();
    }
}
