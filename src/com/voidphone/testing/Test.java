package com.voidphone.testing;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import com.voidphone.testing.Helper.RedirectBackupThread;

public class Test {

	public static void main(String args[]) throws Exception {
		RedirectBackupThread rbt;

		Helper.generateConfig(2);
		newPeer(0);
		newPeer(1);

		Thread.sleep(60000);

		String onionListenAddress = Helper.getPeerConfig(1).config.get("onion", "listen_address", String.class);
		String parameter[] = new String[] { "-c", Helper.getConfigPath(0), "-k",
				Helper.getPeerConfig(1).config.get("onion", "hostkey", String.class), "-p",
				onionListenAddress.substring(onionListenAddress.lastIndexOf(":") + 1) + "", "-t", "127.0.0.1" };
		TestProcess test = new TestProcess(tests.onion.Main.class, Helper.classpath, parameter);
		rbt = new RedirectBackupThread(test.getOut(), -1);
		rbt.start();

		Thread.sleep(60000);

		test.terminate();
	}

	public static void newPeer(int i) throws IOException, InterruptedException {
		HashMap<String, String> properties = new HashMap<String, String>();
		properties.put("keystore.config.file", System.getProperty("keystore.config.file", "security.properties"));
		RedirectBackupThread rbt[] = new RedirectBackupThread[4];
		int j = 0;

		TestProcess gossip = new TestProcess(gossip.Main.class, properties, Helper.classpath,
				new String[] { "-c", Helper.getConfigPath(i) });
		rbt[j] = new RedirectBackupThread(gossip.getOut(), 10 * i + j);
		rbt[j].start();
		j++;
		Thread.sleep(100);
		TestProcess rps = new TestProcess(mockups.rps.Main.class, properties, Helper.classpath,
				new String[] { "-c", Helper.getConfigPath(i) });
		rbt[j] = new RedirectBackupThread(rps.getOut(), 10 * i + j);
		rbt[j].start();
		j++;
		Thread.sleep(100);
		TestProcess auth = new TestProcess(mockups.auth.Main.class, properties, Helper.classpath,
				new String[] { "-c", Helper.getConfigPath(i) });
		rbt[j] = new RedirectBackupThread(auth.getOut(), 10 * i + j);
		rbt[j].start();
		j++;
		Thread.sleep(100);
		TestProcess onion = new TestProcess(com.voidphone.onion.Main.class, properties, Helper.classpath,
				new String[] { "-c", Helper.getConfigPath(i) });
		rbt[j] = new RedirectBackupThread(onion.getOut(), 10 * i + j);
		rbt[j].start();
		j++;
		Thread.sleep(100);
		Helper.info("Launched peer " + i);
	}
}
