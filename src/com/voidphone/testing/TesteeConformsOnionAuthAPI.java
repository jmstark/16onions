package com.voidphone.testing;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.Executors;

import com.voidphone.api.OnionAuthApiSocket;
import com.voidphone.testing.Helper.RedirectBackupThread;

import auth.api.OnionAuthCipherDecryptResp;
import auth.api.OnionAuthCipherEncryptResp;
import auth.api.OnionAuthDecryptResp;
import auth.api.OnionAuthEncryptResp;
import auth.api.OnionAuthSessionHS1;
import auth.api.OnionAuthSessionHS2;
import util.PEMParser;

public class TesteeConformsOnionAuthAPI {
	public static void main(String args[]) throws Exception {
		String addr;
		InetSocketAddress address;
		File file;
		byte message[] = "Hello World!".getBytes();

		Helper.generateConfig(3, 0);
		newPeer(0);
		newPeer(1);

		Thread.sleep(1000);

		Helper.info("Launching test!");
		addr = Helper.getPeerConfig(0).config.get("auth", "api_address", String.class);
		address = new InetSocketAddress(addr.substring(0, addr.lastIndexOf(":")),
				Integer.parseInt(addr.substring(addr.lastIndexOf(":") + 1)));
		OnionAuthApiSocket oaas1 = new OnionAuthApiSocket(address,
				AsynchronousChannelGroup.withFixedThreadPool(1, Executors.defaultThreadFactory()), 5000);
		file = new File(Helper.getPeerConfig(0).config.get("onion", "hostkey", String.class));
		RSAPublicKey hostkey1 = PEMParser.getPublicKeyFromPEM(file);
		addr = Helper.getPeerConfig(1).config.get("auth", "api_address", String.class);
		address = new InetSocketAddress(addr.substring(0, addr.lastIndexOf(":")),
				Integer.parseInt(addr.substring(addr.lastIndexOf(":") + 1)));
		OnionAuthApiSocket oaas2 = new OnionAuthApiSocket(address,
				AsynchronousChannelGroup.withFixedThreadPool(1, Executors.defaultThreadFactory()), 5000);

		int id1 = oaas1.register();
		int id2 = oaas2.register();
		OnionAuthSessionHS1 oashs1 = oaas1.AUTHSESSIONSTART(oaas1.newOnionAuthSessionStartMessage(id1, hostkey1));
		OnionAuthSessionHS2 oashs2 = oaas2
				.AUTHSESSIONINCOMINGHS1(oaas2.newOnionAuthSessionIncomingHS1(id2, oashs1.getPayload()));
		oaas1.AUTHSESSIONINCOMINGHS2(
				oaas1.newOnionAuthSessionIncomingHS2(id1, oashs1.getSessionID(), oashs2.getPayload()));
		OnionAuthEncryptResp oaer = oaas1
				.AUTHLAYERENCRYPT(oaas1.newOnionAuthEncrypt(id1, new int[] { oashs1.getSessionID() }, message));
		OnionAuthDecryptResp oadr = oaas2.AUTHLAYERDECRYPT(
				oaas2.newOnionAuthDecrypt(id2, new int[] { oashs2.getSessionID() }, oaer.getPayload()));
		if (!Arrays.equals(oadr.getPayload(), message)) {
			System.exit(1);
		}
		OnionAuthCipherEncryptResp oacer = oaas1
				.AUTHCIPHERENCRYPT(oaas1.newOnionAuthEncrypt(id1, oashs1.getSessionID(), false, message));
		OnionAuthCipherDecryptResp oacdr = oaas2
				.AUTHCIPHERDECRYPT(oaas2.newOnionAuthDecrypt(id2, oashs2.getSessionID(), oacer.getPayload()));
		if (!Arrays.equals(oacdr.getPayload(), message)) {
			System.exit(1);
		}
		oaas1.AUTHSESSIONCLOSE(oaas1.newOnionAuthClose(id1, oashs1.getSessionID()));
		oaas1.unregister(id1);
		oaas2.AUTHSESSIONCLOSE(oaas2.newOnionAuthClose(id2, oashs2.getSessionID()));
		oaas2.unregister(id2);
		Thread.sleep(1000);
		System.exit(0);
	}

	public static void newPeer(int i) throws IOException, InterruptedException {
		HashMap<String, String> properties = new HashMap<String, String>();
		properties.put("keystore.config.file", System.getProperty("keystore.config.file", "security.properties"));
		RedirectBackupThread rbt[] = new RedirectBackupThread[1];
		int j = 0;

		TestProcess auth = new TestProcess(mockups.auth.Main.class, properties, Helper.classpath,
				new String[] { "-c", Helper.getConfigPath(i) });
		rbt[j] = new RedirectBackupThread(auth.getOut(), 10 * i + j);
		rbt[j].start();
		j++;
		Thread.sleep(100);
		Helper.info("Launched peer " + i);
	}
}
