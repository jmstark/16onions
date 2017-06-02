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
package mockups.auth;

import java.util.Random;
import javax.crypto.ShortBufferException;
import mockups.auth.PartialSessionImpl.SessionImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class PartialSessionImplTest {

    private PartialSessionImpl pa, pb;
    private SessionImpl a, b;
    private static final Random random = new Random();

    public PartialSessionImplTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        pa = new PartialSessionImpl();
        pb = new PartialSessionImpl();
        a = (SessionImpl) pa.completeSession(pb.getOurKeyHalf());
        b = (SessionImpl) pb.completeSession(pa.getOurKeyHalf());
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getID method, of class PartialSessionImpl.
     *
     * When a session is generated from partial, their IDs should not change
     */
    @Test
    public void testGetID() {
        System.out.println("getID");
        assertEquals(pa.getID(), a.getID());
    }

    /**
     * Test of getOurKeyHalf method, of class PartialSessionImpl.
     *
     * When a session is generated from partial, their DH key halves should not
     * change
     */
    @Test
    public void testGetOurKeyHalf() {
        System.out.println("getOurKeyHalf");
        assertEquals(pa.getOurKeyHalf(), a.getOurKeyHalf());
    }

    /**
     * When two partial sessions are used to generate sessions respectively,
     * both the sessions should end up with the same symmetric key.
     */
    @Test
    public void testSharedKeyGeneration() {
        System.out.println("sharedKeyGeneration");
        assertTrue(a.spec.equals(b.spec));
    }

    private byte[] doSingleEncryptDecrypt(byte[] input) throws
            ShortBufferException {
        byte[] cyphertext = a.encrypt(input);
        return b.decrypt(cyphertext);
    }

    /**
     * Test a single session encryption and decryption
     */
    @Test
    public void testSingleEncryptDecrypt() throws ShortBufferException {
        byte[] expected;
        int count = 0;
        do {
            expected = new byte[random.nextInt(64000)];
            random.nextBytes(expected);
            assertArrayEquals(expected, doSingleEncryptDecrypt(expected));
        } while (count++ < 200);
    }

    private PartialSessionImpl[] generatePartialSessions(int size) {
        PartialSessionImpl[] sessions;
        sessions = new PartialSessionImpl[size];
        for (int index = 0; index < size; index++) {
            sessions[index] = new PartialSessionImpl();
        }
        return sessions;
    }

    private byte[] doLayerEncryptDecrypt(byte[] data, int length) throws
            ShortBufferException {
        int index;
        PartialSessionImpl[] partialSessions = generatePartialSessions(
                length * 2);
        Session[] encryptSessions, decryptSessions;
        encryptSessions = new Session[length];
        decryptSessions = new Session[length];
        for (index = 0; index < length; index++) {
            PartialSession pa = partialSessions[2 * index];
            PartialSession pb = partialSessions[2 * index + 1];
            encryptSessions[index] = pa.completeSession(pb.getOurKeyHalf());
            decryptSessions[length - index - 1] = pb.completeSession(pa.
                    getOurKeyHalf());
        }
        for (Session session : encryptSessions) {
            data = session.encrypt(data);
        }
        for (Session session : decryptSessions) {
            data = session.decrypt(data);
        }
        return data;
    }

    @Test
    public void testLayerEncryptDecrypt() throws ShortBufferException {
        byte[] data;
        int count = 0;
        do {
            data = new byte[random.nextInt(64000)];
            random.nextBytes(data);
            assertArrayEquals(data, doLayerEncryptDecrypt(data,
                    random.nextInt(38) + 2));
        } while (count++ < 100);
    }

}
