package com.voidphone.testing;

import java.net.InetSocketAddress;
import java.util.HashMap;

import com.voidphone.api.RpsApiSocket;
import com.voidphone.general.TestProcess;

import rps.api.RpsPeerMessage;

public class TesteeConformsRPSAPI {
	public static void main(String args[]) throws Exception {
		HashMap<String, String> properties = new HashMap<String, String>();
		properties.put("keystore.config.file", "\"" + System.getProperty("user.dir") + "/../.keystore\"");
		String classpath[] = new String[] { System.getProperty("user.dir") + "/testing/libs/commons-cli-1.3.1.jar",
				System.getProperty("user.dir") + "/testing/libs/ini4j-0.5.4.jar", "junit-4.12.jar",
				System.getProperty("user.dir") + "/testing/libs/bcprov-jdk15on-155.jar" };
		String peer0[] = new String[] { "-c", System.getProperty("user.dir") + "/tests/peer0/peer0.conf" };
		String peer1[] = new String[] { "-c", System.getProperty("user.dir") + "/tests/peer1/peer1.conf" };

		TestProcess gossip0 = new TestProcess(gossip.Main.class, properties, classpath, peer0);
		TestProcess rps0 = new TestProcess(mockups.rps.Main.class, properties, classpath, peer0);
		System.out.println("Launched peer 0");
		TestProcess gossip1 = new TestProcess(gossip.Main.class, properties, classpath, peer1);
		TestProcess rps1 = new TestProcess(mockups.rps.Main.class, properties, classpath, peer1);
		System.out.println("Launched peer 1");
		Thread.sleep(20000);
		RpsApiSocket test = new RpsApiSocket(new InetSocketAddress("127.0.0.1", 11101));
		System.out.println("Launched test");
		for (int i = 0; i < 10; i++) {
			RpsPeerMessage rpm = test.RPSQUERY();
			if (rpm != null) {
				System.out.println(rpm.getAddress());
			}
			Thread.sleep(5000);
		}
		// TestProcess test = new TestProcess(tests.rps.Main.class, properties,
		// classpath, peer0);

		// test.terminate();
		rps1.terminate();
		rps0.terminate();
		gossip1.terminate();
		gossip0.terminate();
	}
}
