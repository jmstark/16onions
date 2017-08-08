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
import java.security.interfaces.RSAPublicKey;

/**
 * Interface for tunnel objects.
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 * @param <A> Type for the context
 */
public interface TunnelEventHandler<A> {

    /**
     * Called to ask for a new context to be associated with a new tunnel.
     *
     * This call is called for both tunnels which are created from this peer and
     * also for incoming tunnels.
     *
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
     * @param hostkey the hostkey given to the P2PService.createTunnel() method
     */
    public void tunnelCreated(Tunnel<A> tunnel, RSAPublicKey hostkey);

    /**
     * Tunnel creation has failed.
     *
     * @param exc the exception to indicate why tunnel creation failed
     * @param address
     * @param hostkey the hostkey given to the P2PService.createTunnel() method
     */
    public void tunnelCreatefailed(Throwable exc,
            InetSocketAddress address,
            RSAPublicKey hostkey);

    /**
     * Called when a peer opened a tunnel to us.
     *
     * @param tunnel the new incoming tunnel
     * @param key the public key of the other peer
     */
    public void newIncomingTunnel(Tunnel<A> tunnel);

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
