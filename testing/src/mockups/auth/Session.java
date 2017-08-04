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

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;

/**
 *
 * @author totakura
 */
public interface Session extends PartialSession {

    int getMaxBlockSize();

    /**
     * Encrypt given block of bytes
     *
     * @param data the data to encrypt
     * @param isCipher is the data an encryption of a previous plaintext?
     * @return encrypted block of bytes
     * @throws IllegalBlockSizeException if the size of data is greater than the
     * maximum block size accepted. This is unrelated to the underlying crypto
     * block size. Use getMaxBlockSize() to know the implemented maximum block
     * size.
     */
    byte[] encrypt(boolean isCipher, byte[] data)
            throws IllegalBlockSizeException;

    EncryptDecryptBlock decrypt(byte[] data)
            throws ShortBufferException;
}
