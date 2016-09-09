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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1StreamParser;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class PEMParser {
    private static final KeyFactory factory;

    static {
        try {
            factory = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(
                    "A provider for RSA is not available; cannot continue");
        }
    }

    private static ASN1Encodable readPEM(File file) throws
            IOException {
        try (PemReader reader = new PemReader(new FileReader(file))) {
            PemObject object = reader.readPemObject();
            System.out.println("Read pem object of type: " + object.getType());
            return new ASN1StreamParser(object.getContent()).readObject();
        }
    }

    private static org.bouncycastle.asn1.pkcs.RSAPrivateKey readPKCSPrivateKeyFromPEM(
            File file) throws
            IOException {
        return org.bouncycastle.asn1.pkcs.RSAPrivateKey.getInstance(
                readPEM(file));
    }

    public static RSAPrivateKey getPrivateKeyFromPEM(File file) throws
            IOException, InvalidKeyException {
        org.bouncycastle.asn1.pkcs.RSAPrivateKey key;
        key = readPKCSPrivateKeyFromPEM(file);
        RSAPrivateKeySpec spec;
        spec = new RSAPrivateKeySpec(key.getModulus(), key.getPrivateExponent());
        try {
            return (RSAPrivateKey) factory.generatePrivate(spec);
        } catch (InvalidKeySpecException ex) {
            throw new InvalidKeyException(ex.getMessage());
        }
    }

    public static RSAPublicKey getPublicKeyFromPEM(File file) throws
            IOException, InvalidKeyException {
        org.bouncycastle.asn1.pkcs.RSAPrivateKey priv = readPKCSPrivateKeyFromPEM(
                file);
        RSAPublicKeySpec spec = new RSAPublicKeySpec(priv.getModulus(), priv.
                getPublicExponent());
        try {
            return (RSAPublicKey) factory.generatePublic(spec);
        } catch (InvalidKeySpecException ex) {
            throw new InvalidKeyException(ex.getMessage());
        }
    }

    public static void main(String[] args) throws IOException,
            InvalidKeyException {
        File file = new File("/tmp/only_private.pem");
        RSAPrivateKey priv = getPrivateKeyFromPEM(file);
        RSAPublicKey pub = getPublicKeyFromPEM(file);
    }
}
