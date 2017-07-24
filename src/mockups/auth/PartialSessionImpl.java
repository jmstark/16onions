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

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class PartialSessionImpl implements PartialSession {

    private static int ID = 0;
    protected final int id;
    protected static final int KEY_SIZE = 128 / 8; //size in bytes
    protected final byte[] ourKeyBytes;
    protected final Random random;

    public PartialSessionImpl() {
        random = new Random();
        ourKeyBytes = new byte[KEY_SIZE];
        this.id = ID++;
        random.nextBytes(ourKeyBytes);
    }

    /**
     * Copy constructor for internal use.
     *
     * @param partial the partial session to copy from
     */
    protected PartialSessionImpl(PartialSessionImpl partial) {
        this.id = partial.id;
        this.ourKeyBytes = partial.ourKeyBytes;
        this.random = partial.random;
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

    protected static class SessionImpl extends PartialSessionImpl implements
            Session {

        private static final String CIPHER_TRANSFORMATION = "AES/CBC/NoPadding";
        private static final short magic = (short) 34884;
        private static final int maxBlockSize = 20 * 1024;
        protected final SecretKeySpec spec;

        private SessionImpl(PartialSessionImpl partial, Key otherKey) {
            super(partial);

            byte[] key;
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
            key = new byte[keySize];
            for (int index = 0; index < keySize; index++) {
                key[index] = (byte) ((index < keySize)
                        ? a[index] ^ b[index] : b[index]);
            }
            this.spec = new SecretKeySpec(key, "AES");

        }

        @Override
        public int getMaxBlockSize() {
            // max size - iv size - magic - size
            return maxBlockSize - KEY_SIZE - 2 - 2;
        }

        @Override
        public byte[] encrypt(boolean isCipher, byte[] data) throws
                IllegalBlockSizeException {
            byte[] iv;
            byte[] block;
            IvParameterSpec ivSpec;
            Cipher cipher;

            if (isCipher) {
                if (data.length != maxBlockSize) {
                    throw new IllegalBlockSizeException();
                }
                iv = Arrays.copyOf(data, KEY_SIZE);
                block = Arrays.copyOfRange(data, KEY_SIZE, data.length);
            } else {
                if (maxBlockSize < data.length + KEY_SIZE + 2 + 2) {
                    throw new IllegalBlockSizeException();
                }
                iv = new byte[KEY_SIZE];
                random.nextBytes(iv);
                ByteBuffer buf = ByteBuffer.allocate(maxBlockSize - iv.length);
                assert (buf.hasArray());
                buf.putShort(magic);
                buf.putShort((short) data.length);
                buf.put(data);
                buf.flip(); //not strictly needed, but to keep up with practise
                block = buf.array();
            }
            ivSpec = new IvParameterSpec(iv);

            try {
                cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            } catch (NoSuchAlgorithmException | NoSuchPaddingException ex) {
                throw new RuntimeException(); //AES CBC w Padding has to be available
            }
            try {
                cipher.init(Cipher.ENCRYPT_MODE, spec, ivSpec);
            } catch (InvalidKeyException | InvalidAlgorithmParameterException ex) {
                throw new RuntimeException(); // this should not happen
            }
            //first encrypt the IV in ECB mode
            ArrayList output = new ArrayList(maxBlockSize);

            //then use the resulting IV to encrypt data

            output = Arrays.copyOf(iv, maxBlockSize);
            try {
                cipher.doFinal(block, 0, block.length, output, iv.length);
            } catch (ShortBufferException
                    | BadPaddingException
                    | IllegalBlockSizeException ex) {
                //ShortBufferException: we made sure that the buffer is long enough
                //IllegalBlockSizeException: our block is always *1024 bytes
                //BadPaddingException: we are not decrypting here
                throw new RuntimeException();
            }
            return output;
        }

        @Override
        public EncryptDecryptBlock decrypt(byte[] data) throws
                ShortBufferException {
            Cipher cipher;
            byte[] iv;
            byte[] output;
            IvParameterSpec ivSpec;

            if (data.length != maxBlockSize) {
                throw new ShortBufferException();
            }
            //extract IV and initialize it
            iv = Arrays.copyOf(data, KEY_SIZE);
            ivSpec = new IvParameterSpec(iv);

            try {
                cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            } catch (NoSuchAlgorithmException | NoSuchPaddingException ex) {
                throw new RuntimeException(); //AES CBC has to be available
            }
            try {
                cipher.init(Cipher.DECRYPT_MODE, spec, ivSpec);
            } catch (InvalidKeyException | InvalidAlgorithmParameterException ex) {
                throw new RuntimeException(); // this should not happen
            }
            try {
                output = cipher.doFinal(data, KEY_SIZE, data.length - KEY_SIZE);
            } catch (IllegalBlockSizeException | BadPaddingException ex) {
                //cipher spec takes care of padding
                throw new RuntimeException();
            }
            ByteBuffer outBuffer;
            outBuffer = ByteBuffer.wrap(output);
            outBuffer.position(KEY_SIZE); //skip IV
            if (magic != outBuffer.getShort()) {
                return new EncryptDecryptBlock(true, output);
            }
            short size;
            int headerSize = maxBlockSize - getMaxBlockSize();
            size = outBuffer.getShort();
            if (getMaxBlockSize() < size) {
                return new EncryptDecryptBlock(true, output);
            }
            byte[] zeros = Arrays.copyOf(output, headerSize + size);
            for (byte b : zeros) {
                if ((byte) 0 != b) {
                    return new EncryptDecryptBlock(true, output);
                }
            }
            return new EncryptDecryptBlock(false,
                    Arrays.copyOfRange(output, headerSize, headerSize + size));
        }

    }
}
