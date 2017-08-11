package com.voidphone.testing;

import java.io.IOException;
import java.util.HashMap;

import com.voidphone.testing.Helper.RedirectBackupThread;

public class Test {

	public static void main(String args[]) throws Exception {

		Helper.generateConfig(6);
		newPeer(0);
		newPeer(1);
		newPeer(2);
		newPeer(3);
		newPeer(4);
		newPeer(5);

		Thread.sleep(60000);

		newCM(1, 4);
		newCM(3, 2);
		newCM(5, 0);

		Thread.sleep(60000);
	}

	public static void newCM(int i, int j) throws IOException {
		RedirectBackupThread rbt;
		String address;
		String parameter[];
		TestProcess test;

		address = Helper.getPeerConfig(j).config.get("onion", "listen_address", String.class);
		parameter = new String[] { "-c", Helper.getConfigPath(i), "-k",
				Helper.getPeerConfig(j).config.get("onion", "hostkey", String.class), "-p",
				address.substring(address.lastIndexOf(":") + 1) + "", "-t",
				address.substring(0, address.lastIndexOf(":")) };
		test = new TestProcess(tests.onion.Main.class, Helper.classpath, parameter);
		rbt = new RedirectBackupThread(test.getOut(), 5);
		rbt.start();

		address = Helper.getPeerConfig(i).config.get("onion", "listen_address", String.class);
		parameter = new String[] { "-c", Helper.getConfigPath(j), "-k",
				Helper.getPeerConfig(i).config.get("onion", "hostkey", String.class), "-p",
				address.substring(address.lastIndexOf(":") + 1) + "", "-t",
				address.substring(0, address.lastIndexOf(":")), "-l" };
		test = new TestProcess(tests.onion.Main.class, Helper.classpath, parameter);
		rbt = new RedirectBackupThread(test.getOut(), 5);
		rbt.start();
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
