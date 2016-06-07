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

import java.util.Arrays;
import java.util.HashSet;

/**
 * Abstract Item which implements handling of known peers of the Item interface
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

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 37 * hash + this.getType();
        hash = 37 * hash + Arrays.hashCode(this.getData());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Item other = (Item) obj;
        if (this.getType() != other.getType()) {
            return false;
        }
        if (!Arrays.equals(this.getData(), other.getData())) {
            return false;
        }
        return true;
    }

}
