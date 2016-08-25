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

import java.security.InvalidParameterException;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class SecurityHelperTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static final KeyPair keyPair = SecurityHelper.generateRSAKeyPair(
            2048);
    private byte[] pubKeyEnc;
    private byte[] secKeyEnc;

    public SecurityHelperTest() {
    }

    /**
     * Test of encodeRSAPublicKey method, of class SecurityHelper.
     */
    @Test
    public void testEncodeRSAPublicKey() {
        System.out.println("encodeRSAPublicKey");
        pubKeyEnc = SecurityHelper.encodeRSAPublicKey(keyPair.getPublic());
    }

    /**
     * Test of getRSAPublicKeyFromEncoding method, of class SecurityHelper.
     */
    @Test
    public void testGetRSAPublicKeyFromEncoding() throws Exception {
        System.out.println("getRSAPublicKeyFromEncoding");
        testEncodeRSAPublicKey();
        RSAPublicKey expResult = (RSAPublicKey) keyPair.getPublic();
        RSAPublicKey result = SecurityHelper.getRSAPublicKeyFromEncoding(
                pubKeyEnc);
        assertEquals(expResult, result);
    }

    /**
     * Test of encodeRSAPrivateKey method, of class SecurityHelper.
     */
    @Test
    public void testEncodeRSAPrivateKey() {
        System.out.println("encodeRSAPrivateKey");
        secKeyEnc = SecurityHelper.encodeRSAPrivateKey(keyPair.getPrivate());
    }

    /**
     * Test of getRSAPrivateKeyFromEncoding method, of class SecurityHelper.
     */
    @Test
    public void testGetRSAPrivateKeyFromEncoding() throws Exception {
        System.out.println("getRSAPrivateKeyFromEncoding");
        testEncodeRSAPrivateKey();
        RSAPrivateKey expResult = (RSAPrivateKey) keyPair.getPrivate();
        RSAPrivateKey result = SecurityHelper.getRSAPrivateKeyFromEncoding(
                secKeyEnc);
        assertEquals(expResult, result);
    }

    /**
     * Test of generateRSAKeyPair method, of class SecurityHelper.
     */
    @Test
    public void testGenerateRSAKeyPair() {
        System.out.println("generateRSAKeyPair");
        SecurityHelper.generateRSAKeyPair(1024);
        SecurityHelper.generateRSAKeyPair(2048);

        thrown.expect(InvalidParameterException.class);
        SecurityHelper.generateRSAKeyPair(340);//some number which is not 1024 or 2048
    }
}
