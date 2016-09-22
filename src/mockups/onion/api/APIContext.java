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

import mockups.onion.p2p.TunnelEventHandler;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public interface APIContext {
    /**
     * Destory the context by closing all onion p2p connections associated with
     * this client.
     */
    public void destroy();

    /**
     * Remove the tunnel with the given ID.
     *
     * @param id
     */
    public void removeTunnel(int id);

}
