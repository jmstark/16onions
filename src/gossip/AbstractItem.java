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
package gossip;

import java.util.HashSet;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public abstract class AbstractItem implements Item {
    private final HashSet<Peer> peers;

    public AbstractItem() {
        this.peers = new HashSet(30); //FIXME: Better default? Perhaps the max number of neighbours
    }

    @Override
    public boolean isKnownTo(Peer peer) {
        return peers.contains(peer);
    }

    @Override
    public void knownTo(Peer peer) {
        peers.add(peer);
    }

}
