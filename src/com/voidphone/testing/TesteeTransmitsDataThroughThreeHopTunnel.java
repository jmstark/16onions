package com.voidphone.testing;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import com.voidphone.testing.Helper.RedirectBackupThread;

public class TesteeTransmitsDataThroughThreeHopTunnel {
	public static void main(String args[]) throws Exception {
		Helper.generateConfig(5, 3);
		newPeer(0);
		newPeer(1);
		newPeer(2);
		newPeer(3);
		newPeer(4);

		Thread.sleep(60000);

		Helper.info("Launching test!");
		System.exit(newCM(0, 4).waitFor(30, TimeUnit.SECONDS));
	}

	public static TestProcess newCM(int i, int j) throws IOException {
		RedirectBackupThread rbt;
		String address;
		String parameter[];
		TestProcess test;

		address = Helper.getPeerConfig(j).config.get("onion", "listen_address", String.class);
		parameter = new String[] { "-c", Helper.getConfigPath(i), "-d", Helper.getConfigPath(j), "-k",
				Helper.getPeerConfig(j).config.get("onion", "hostkey", String.class), "-p",
				address.substring(address.lastIndexOf(":") + 1), "-t", address.substring(0, address.lastIndexOf(":")) };
		test = new TestProcess(com.voidphone.testing.tests.onion.Main.class, Helper.classpath, parameter);
		rbt = new RedirectBackupThread(test.getOut(), 10 * i + 5);
		rbt.start();
		return test;
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
