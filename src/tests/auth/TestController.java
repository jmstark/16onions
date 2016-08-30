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

import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import tools.SecurityHelper;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class TestController {

    private final Context context;
    private final ScheduledExecutorService scheduledExecutor;

    public TestController(Context context,
            ScheduledExecutorService scheduledExecutor) {
        this.context = context;
        this.scheduledExecutor = scheduledExecutor;
    }

    public void start() throws Exception {
        KeyPair pair1 = SecurityHelper.generateRSAKeyPair(1024);
        KeyPair pair2 = SecurityHelper.generateRSAKeyPair(1024);
        PartialSession partial1;
        PartialSession partial2;
        {
            Future<PartialSession> future = context.startSession(
                    (RSAPublicKey) pair1.getPublic(), null);
            partial1 = future.get();
        }
        {
            Future<PartialSession> future = context.startSession(
                    (RSAPublicKey) pair2.getPublic(), null);
            partial2 = future.get();
        }
        Session session1 = partial1.completeSession(partial2.getDiffiePayload());
        Session session2 = partial2.completeSession(partial1.getDiffiePayload());

        Tunnel tunnel1 = context.createTunnel(session1);
        Tunnel tunnel2 = context.createTunnel(session2);

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
    }

}
