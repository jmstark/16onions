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
package tests.auth;

import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import protocol.MessageSizeExceededException;
import util.SecurityHelper;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class TestController {

    private final Context context1;
    private final ScheduledExecutorService scheduledExecutor;
    private final Context context2;

    public TestController(Context context1,
            Context context2,
            ScheduledExecutorService scheduledExecutor) {
        this.context1 = context1;
        this.context2 = context2;
        this.scheduledExecutor = scheduledExecutor;
    }

    public void start() throws Exception {
        KeyStore keyStore = SecurityHelper.keyStore;
        if (keyStore.size() < 2) {
            throw new RuntimeException("keystore needs to have atleast two keys");
        }
        Certificate cert1 = keyStore.getCertificate("sel.A");
        Certificate cert2 = keyStore.getCertificate("hostkey.A");

        PartialSession partial1;
        PartialSession partial2;
        {
            Future<PartialSession> future = context1.startSession(
                    (RSAPublicKey) cert2.getPublicKey(), null);
            partial1 = future.get();
        }
        {
            Future<PartialSession> future = context2.deriveSession(
                    (RSAPublicKey) cert1.getPublicKey(), partial1.
                    getDiffiePayload(), null);
            partial2 = future.get();
        }
        Session session1;
        Session session2;
        session1 = partial1.completeSession(partial2.getDiffiePayload());
        session2 = partial2.completeSession(partial1.getDiffiePayload());

        Tunnel tunnel1 = context1.createTunnel(session1);
        Tunnel tunnel2 = context2.createTunnel(session2);

        String cleartext = "Hello World";
        byte[] encrypted;
        {
            Future<byte[]> future = tunnel1.encrypt(cleartext.getBytes(), null);
            encrypted = future.get();
        }
        byte[] decrypted;
        {
            Future<byte[]> future = tunnel2.decrypt(encrypted, null);
            decrypted = future.get();
        }
        String decryptedText = new String(decrypted);
        System.out.println("Decrypted: " + decryptedText);

        Tunnel t1, t2;
        {
            Future<PartialSession>[] fA, fB;
            Session[] A, B;
            fA = new Future[15];
            fB = new Future[fA.length];
            A = new Session[fA.length];
            B = new Session[fB.length];
            int index;
            for (index = 0; index < fA.length; index++) {
                fA[index] = context2.startSession((RSAPublicKey) cert1.
                        getPublicKey(), null);
                PartialSession pa = fA[index].get();
                fB[index] = context1.deriveSession((RSAPublicKey) cert2.
                        getPublicKey(), pa.getDiffiePayload(), null);
                PartialSession pb = fB[index].get();
                A[index] = pa.completeSession(pb.getDiffiePayload());
                B[index] = pb.completeSession(pa.getDiffiePayload());
            }
            t1 = context2.createTunnel(A[0]);
            t2 = context1.createTunnel(B[0]);
            for (index = 1; index < A.length; index++) {
                t1.addHop(A[index]);
                t2.addHop(B[index]);
            }
        }
        {
            byte[] data = tunnelEncrypt(t1, "hello world".getBytes());
            data = tunnelDecrypt(t2, data);
            System.out.println("Decrypted: " + new String(data));
        }
    }

    private byte[] tunnelEncrypt(Tunnel tunnel, byte[] data) throws
            MessageSizeExceededException, InterruptedException,
            ExecutionException {
        Future<byte[]> f = tunnel.encrypt(data, null);
        return f.get();
    }

    private byte[] tunnelDecrypt(Tunnel tunnel, byte[] data) throws
            MessageSizeExceededException, InterruptedException,
            ExecutionException {
        Future<byte[]> f = tunnel.decrypt(data, null);
        return f.get();
    }
}
