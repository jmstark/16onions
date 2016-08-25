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
package tools;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class SecurityHelper {

    private static final Logger LOGGER = Logger.getLogger("SecurityHelper");
    private static final KeyFactory factory;

    static {
        try {
            factory = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage());
            throw new RuntimeException(
                    "A provider for RSA not available; cannot continue.");
        }
    }

    public static byte[] encodeRSAPublicKey(PublicKey pkey) {
        X509EncodedKeySpec spec;
        try {
            spec = factory.getKeySpec(pkey,
                    X509EncodedKeySpec.class);
        } catch (InvalidKeySpecException ex) {
            throw new InvalidParameterException(
                    "Given parameter is not a RSA public key");
        }
        return spec.getEncoded();
    }

    public static RSAPublicKey getRSAPublicKeyFromEncoding(byte[] encoding)
            throws InvalidKeyException {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(encoding);
        PublicKey pkey;
        try {
            pkey = factory.generatePublic(spec);
        } catch (InvalidKeySpecException ex) {
            throw new InvalidKeyException();
        }
        return (RSAPublicKey) pkey;
    }

    public static byte[] encodeRSAPrivateKey(PrivateKey skey) {
        PKCS8EncodedKeySpec spec;
        try {
            spec = factory.getKeySpec(skey, PKCS8EncodedKeySpec.class);
        } catch (InvalidKeySpecException ex) {
            throw new InvalidParameterException(
                    "Given parameter is not a RSA private key");
        }
        return spec.getEncoded();
    }

    public static RSAPrivateKey getRSAPrivateKeyFromEncoding(byte[] encoding)
            throws InvalidKeyException {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(encoding);
        PrivateKey skey;
        try {
            skey = factory.generatePrivate(spec);
        } catch (InvalidKeySpecException ex) {
            throw new InvalidKeyException();
        }
        return (RSAPrivateKey) skey;
    }

    public static KeyPair generateRSAKeyPair(int keysize) throws
            InvalidParameterException {
        KeyPairGenerator gen;
        try {
            gen = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(
                    "A provider for RSA not available; cannot continue.");
        }
        gen.initialize(keysize);
        return gen.generateKeyPair();
    }

    public static void main(String[] args) throws KeyStoreException, IOException,
            NoSuchAlgorithmException, CertificateException,
            InvalidKeySpecException {

        // This is how KeyStore works
        KeyStore ks = KeyStore.getInstance("jks");
        ks.load(null, null);
        LOGGER.log(Level.INFO, "Entries in KeyStore: {0}", ks.size());

        //Let's try to generate a key pair
        KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA");
        kpGen.initialize(2048);
        KeyPair kp = kpGen.generateKeyPair();
        PublicKey pkey = kp.getPublic();
        PrivateKey skey = kp.getPrivate();

        //Test the key encoding/decoding functions
        byte[] pkeyEnc = encodeRSAPublicKey(pkey);
        byte[] skeyEnc = encodeRSAPrivateKey(skey);
        RSAPublicKey dup_pkey;
        RSAPrivateKey dup_skey;
        try {
            dup_pkey = getRSAPublicKeyFromEncoding(pkeyEnc);
            dup_skey = getRSAPrivateKeyFromEncoding(skeyEnc);
        } catch (InvalidKeyException ex) {
            Logger.getLogger(SecurityHelper.class.getName()).
                    log(Level.SEVERE, null, ex);
            return;
        }

        assert (pkey.equals(dup_pkey));
        assert (skey.equals(dup_skey));
    }
}
