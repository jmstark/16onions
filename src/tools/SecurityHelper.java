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

    public static byte[] encodeRSAPublicKey(PublicKey pkey) {
        return null;
    }

    public static RSAPublicKey getRSAPublicKeyFromEncoding(byte[] encoding) {
        return null;
    }

    public static byte[] encodeRSAPrivateKey(PrivateKey skey) {
        return null;
    }

    public static RSAPrivateKey getRSAPrivateKeyFromEncoding(byte[] encoding) {
        return null;
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

        //Convert the keys into encoded formats.  For this we need KeyFactory
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec pkeyEnc = keyFactory.getKeySpec(pkey,
                X509EncodedKeySpec.class);

        PKCS8EncodedKeySpec skeyEnc = keyFactory.getKeySpec(skey,
                PKCS8EncodedKeySpec.class);
        LOGGER.log(Level.FINE, "KeySpec format for public key: {0}",
                pkeyEnc.getFormat());
        LOGGER.log(Level.FINE, "KeySpec format for private key: {0}",
                skeyEnc.getFormat());
    }
}
