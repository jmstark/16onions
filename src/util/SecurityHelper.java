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
package util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
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
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.DestroyFailedException;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class SecurityHelper {

    private static final Logger LOGGER = Logger.getLogger("SecurityHelper");
    private static final KeyFactory FACTORY;
    public static final KeyStore KEY_STORE;

    static {
        KeyStore keyStore;
        try {
            FACTORY = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage());
            throw new RuntimeException(
                    "A provider for RSA not available; cannot continue.");
        }
        String propertiesPath = System.getProperty("keystore.config.file",
                "security.properties");

        Properties properties;
        properties = new Properties();
        try (FileInputStream fis = new FileInputStream(propertiesPath)) {
            properties.load(fis);
        } catch (FileNotFoundException ex) {
            throw new RuntimeException(
                    "Could not load security properties. See `Security' in README");
        } catch (IOException ex) {
            throw new RuntimeException(ex.toString());
        }
        String ksType = properties.getProperty("keystore.type", KeyStore.
                getDefaultType());
        String ksFile = properties.getProperty("keystore.path", ".keystore");
        String ksPassFile = properties.getProperty("keystore.passwd.path",
                ".keystore_passwd");
        PasswordProtection ksPass;
        try {
            keyStore = KeyStore.getInstance(ksType);
        } catch (KeyStoreException ex) {
            throw new RuntimeException(
                    "Could not load the default keystore; cannot continue");
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(ksPassFile)))) {
            ksPass = new PasswordProtection(reader.readLine().toCharArray());

        } catch (IOException ex) {
            ksPass = new PasswordProtection(new char[0]);
        }
        try (FileInputStream input = new FileInputStream(ksFile)) {
            keyStore.load(input, ksPass.getPassword());
        } catch (FileNotFoundException ex) {
            keyStore = null;
        } catch (IOException | NoSuchAlgorithmException | CertificateException ex) {
            throw new RuntimeException(ex.toString());
        } finally {
            try {
                ksPass.destroy();
            } catch (DestroyFailedException ex) {
                Logger.getLogger(SecurityHelper.class.getName()).
                        log(Level.SEVERE, null, ex);
            }
        }
        KEY_STORE = keyStore;
    }

    public static byte[] encodeRSAPublicKey(PublicKey pkey) {
        X509EncodedKeySpec spec;
        try {
            spec = FACTORY.getKeySpec(pkey,
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
            pkey = FACTORY.generatePublic(spec);
        } catch (InvalidKeySpecException ex) {
            throw new InvalidKeyException();
        }
        return (RSAPublicKey) pkey;
    }

    public static byte[] encodeRSAPrivateKey(PrivateKey skey) {
        PKCS8EncodedKeySpec spec;
        try {
            spec = FACTORY.getKeySpec(skey, PKCS8EncodedKeySpec.class);
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
            skey = FACTORY.generatePrivate(spec);
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
}
