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

import java.net.InetSocketAddress;

/**
 * Interface for tunnel objects.
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 * @param <A> Type for the context
 * @param <B> Type for the attachment
 */
public interface TunnelEventHandler<A, B> {

    /**
     * Called to ask for a new context to be associated with a new tunnel
     * @return
     */
    public A newContext();

    /**
     * Called to indicate that the tunnel creation has been completed.
     *
     * The call should respond with a number indicating the ID given to refer to
     * this tunnel.
     *
     * @param tunnel
     * @param attachment
     */
    public void tunnelCreated(Tunnel<A> tunnel, B attachment);

    /**
     * Tunnel creation has failed.
     *
     * @param exc the exception to indicate why tunnel creation failed
     * @param address
     * @param attachment
     */
    public void tunnelCreatefailed(Throwable exc,
            InetSocketAddress address,
            B attachment);

    /**
     * Function called for data that is received from the P2P connection.
     *
     * The data has to be sent to the upper level application through the API
     * connection.
     *
     * @param tunnel
     * @param data
     */
    public void handleReceivedData(Tunnel<A> tunnel, byte[] data);

    /**
     * Handle a disconnect of this tunnel
     * @param tunnel
     */
    public void handleDisconnect(Tunnel<A> tunnel);
}
