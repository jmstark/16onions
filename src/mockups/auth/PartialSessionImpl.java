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

    private static final int KEY_SIZE = 128; //size in bits
    private static int ID = 0;
    protected final int id;
    protected final byte[] ourKeyBytes;

    public PartialSessionImpl() {
        Random rand = new Random();
        ourKeyBytes = new byte[KEY_SIZE / 8];
        this.id = ID++;
        rand.nextBytes(ourKeyBytes);
    }

    /**
     * Copy constructor for internal use.
     *
     * @param partial the partial session to copy from
     */
    protected PartialSessionImpl(PartialSessionImpl partial) {
        this.id = partial.id;
        this.ourKeyBytes = partial.ourKeyBytes;
    }

    @Override
    public int getID() {
        return this.id;
    }

    @Override
    public Key getOurKeyHalf() {
        return new KeyImpl(ourKeyBytes);
    }

    @Override
    public Session completeSession(Key otherKey) {
        return new SessionImpl(this, otherKey);
    }

    private static class SessionImpl extends PartialSessionImpl implements Session {

        private final byte[] key;

        private SessionImpl(PartialSessionImpl partial, Key otherKey) {
            super(partial);

            byte[] a;
            byte[] b;
            int keySize;

            a = partial.ourKeyBytes;
            b = otherKey.getBytes();
            if (b.length < a.length) {
                byte[] temp = a;
                a = b;
                b = temp;
            }
            keySize = b.length;
            this.key = new byte[keySize];
            for (int index = 0; index < keySize; index++) {
                this.key[index] = (byte) ((index < keySize)
                        ? a[index] ^ b[index] : b[index]);
            }
        }

        @Override
        public byte[] encrypt(byte[] data) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public byte[] decrypt(byte[] data) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }
}
