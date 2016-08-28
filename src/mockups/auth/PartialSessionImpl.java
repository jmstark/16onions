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
package mockups.auth;

import java.util.Random;

public class PartialSessionImpl implements PartialSession {

    private static final int KEY_SIZE = 512; //size in bits
    private static int ID = 0;
    protected final int id;
    protected final byte[] ourKeyBytes;

    public PartialSessionImpl() {
        Random rand = new Random();
        ourKeyBytes = new byte[KEY_SIZE / 8];
        this.id = ID++;
        rand.nextBytes(ourKeyBytes);
    }

    @Override
    public int getID() {
        return this.id;
    }

    @Override
    public Key getOurKeyHalf() {
        return new KeyImpl(ourKeyBytes);
    }
}
