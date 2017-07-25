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
import java.nio.charset.Charset;
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
        private static final IvParameterSpec magicIVSpec;
        private static final Cipher ivCipher;
        private static final short MAGIC = (short) 34884;
        private static final int MAX_BLOCK_SIZE = 20 * 1024;
        protected final SecretKeySpec spec;

        static {
            byte[] iv = "1234567890ABCDEF".getBytes(Charset.forName("US-ASCII"));
            magicIVSpec = new IvParameterSpec(iv);
            String IV_TRANSFORMATION = "AES/ECB/NoPadding";
            try {
                ivCipher = Cipher.getInstance(IV_TRANSFORMATION);
            } catch (NoSuchAlgorithmException | NoSuchPaddingException ex) {
                Logger.getLogger(PartialSessionImpl.class.getName()).
                        log(Level.SEVERE, null, ex);
                throw new RuntimeException();
            }

        }

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
            // max size - iv size - MAGIC - size
            return MAX_BLOCK_SIZE - KEY_SIZE - 2 - 2;
        }

        private byte[] encryptIV(byte[] iv) throws IllegalBlockSizeException {
            try {
                ivCipher.init(Cipher.ENCRYPT_MODE, spec);
            } catch (InvalidKeyException ex) {
                throw new RuntimeException(ex); // this should not happen
            }
            byte[] output = null;
            try {
                output = ivCipher.doFinal(iv);
            } catch (BadPaddingException ex) {
                throw new RuntimeException(ex); //this should not happen
            }
            return output;
        }

        private byte[] decryptIV(byte[] iv) throws IllegalBlockSizeException {
            try {
                ivCipher.init(Cipher.DECRYPT_MODE, spec);
            } catch (InvalidKeyException ex) {
                throw new RuntimeException(ex); // this should not happen
            }
            byte[] output = null;
            try {
                output = ivCipher.doFinal(iv);
            } catch (BadPaddingException ex) {
                throw new RuntimeException(ex); //this should not happen
            }
            return output;
        }
        @Override
        public byte[] encrypt(boolean isCipher, byte[] data) throws
                IllegalBlockSizeException {
            byte[] iv;
            byte[] block;
            IvParameterSpec ivSpec;
            Cipher cipher;

            if (isCipher) {
                if (data.length != MAX_BLOCK_SIZE) {
                    throw new IllegalBlockSizeException();
                }
                iv = Arrays.copyOf(data, KEY_SIZE);
                block = Arrays.copyOfRange(data, KEY_SIZE, data.length);
            } else {
                if (MAX_BLOCK_SIZE < data.length + KEY_SIZE + 2 + 2) {
                    throw new IllegalBlockSizeException();
                }
                iv = new byte[KEY_SIZE];
                random.nextBytes(iv);
                ByteBuffer buf = ByteBuffer.allocate(MAX_BLOCK_SIZE - iv.length);
                assert (buf.hasArray());
                buf.putShort(MAGIC);
                buf.putShort((short) data.length);
                buf.put(data);
                buf.flip(); //not strictly needed, but to keep up with practise
                block = buf.array();
            }
            iv = encryptIV(iv);
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

            //then use the resulting IV to encrypt data
            byte[] enc = null;
            try {
                enc = cipher.doFinal(block);
            } catch (BadPaddingException
                    | IllegalBlockSizeException ex) {
                //ShortBufferException: we made sure that the buffer is long enough
                //IllegalBlockSizeException: our block is always *1024 bytes
                //BadPaddingException: we are not decrypting here
                throw new RuntimeException();
            }
            ByteBuffer out = ByteBuffer.allocate(MAX_BLOCK_SIZE);
            out.put(iv);
            out.put(enc);
            out.flip();
            return out.array();
        }

        @Override
        public EncryptDecryptBlock decrypt(byte[] data) throws
                ShortBufferException {
            Cipher cipher;
            byte[] iv;
            byte[] output;
            IvParameterSpec ivSpec;

            if (data.length != MAX_BLOCK_SIZE) {
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
            try {
                iv = decryptIV(iv);
            } catch (IllegalBlockSizeException ex) {
                throw new RuntimeException(ex); //should not happen
            }
            ByteBuffer outBuffer;
            outBuffer = ByteBuffer.allocate(MAX_BLOCK_SIZE);
            outBuffer.put(iv);
            outBuffer.put(output);
            outBuffer.flip();
            outBuffer.position(KEY_SIZE);
            outBuffer.mark();
            if (MAGIC != outBuffer.getShort()) {
                return new EncryptDecryptBlock(true, outBuffer.array());
            }
            short size;
            size = outBuffer.getShort();
            if (getMaxBlockSize() < size) {
                return new EncryptDecryptBlock(true, outBuffer.array());
            }
            outBuffer.position(outBuffer.position() + size);
            //check if the remaining data is zeros or not
            while (outBuffer.hasRemaining()) {
                if (outBuffer.get() != 0) {
                    return new EncryptDecryptBlock(true, outBuffer.array());
                }
            }
            return new EncryptDecryptBlock(false,
                    Arrays.copyOfRange(output, 4, size + 4));
        }
    }
}
