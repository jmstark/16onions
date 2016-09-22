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
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;

/**
 * P2PService Module for Onion.
 *
 * This handles the P2PService connections; both outgoing and incoming.
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public interface P2PService {
    /**
     * Create a tunnel to the given address.
     *
     * The tunnel endpoint should be authenticated with the given hostkey.
     *
     * @param <A>
     * @param group
     * @param address
     * @param attachment
     * @param handler
     * @throws java.io.IOException
     */
    public <A, B> void createTunnel(AsynchronousChannelGroup group,
            InetSocketAddress address,
            B attachment,
            TunnelEventHandler<A, B> handler) throws IOException;
}
