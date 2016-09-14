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
package mockups.rps;

import java.net.InetSocketAddress;
import java.security.InvalidKeyException;
import java.security.interfaces.RSAPublicKey;
import java.util.Objects;
import util.SecurityHelper;

/**
 *
 * @author totakura
 */
public class OnionAddress {

    private final InetSocketAddress address;
    private final RSAPublicKey hostkey;

    public OnionAddress(InetSocketAddress address, byte[] keyEncoding)
            throws InvalidKeyException {
        this.address = address;
        this.hostkey = SecurityHelper.getRSAPublicKeyFromEncoding(keyEncoding);
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public RSAPublicKey getHostKey() {
        return hostkey;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + Objects.hashCode(this.address);
        hash = 47 * hash + Objects.hashCode(this.hostkey);
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
        final OnionAddress other = (OnionAddress) obj;
        if (!Objects.equals(this.address, other.address)) {
            return false;
        }
        if (!Objects.equals(this.hostkey, other.hostkey)) {
            return false;
        }
        return true;
    }
}
