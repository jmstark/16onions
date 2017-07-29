/*
 * Copyright (c) 2017, Charlie Groh and Josef Stark. All rights reserved.
 * 
 * This file is part of 16onions.
 *
 * 16onions is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * 16onions is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with 16onions.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.voidphone.testing;

import java.net.InetSocketAddress;
import java.util.HashMap;

import com.voidphone.api.RpsApiSocket;
import com.voidphone.general.General;
import com.voidphone.testing.TestProcess;

import rps.api.RpsPeerMessage;

public class TesteeConformsRPSAPI {
	public static void main(String args[]) throws Exception {
		HashMap<String, String> properties = new HashMap<String, String>();
		properties.put("keystore.config.file", System.getProperty("user.dir") + "/../.keystore");
		String classpath[] = new String[] { System.getProperty("user.dir") + "/testing/libs/commons-cli-1.3.1.jar",
				System.getProperty("user.dir") + "/testing/libs/ini4j-0.5.4.jar", "junit-4.12.jar",
				System.getProperty("user.dir") + "/testing/libs/bcprov-jdk15on-155.jar" };
		String peer0[] = new String[] { "-c", System.getProperty("user.dir") + "/tests/peer0/peer0.conf" };
		String peer1[] = new String[] { "-c", System.getProperty("user.dir") + "/tests/peer1/peer1.conf" };

		TestProcess gossip0 = new TestProcess(gossip.Main.class, properties, classpath, peer0);
		TestProcess rps0 = new TestProcess(mockups.rps.Main.class, properties, classpath, peer0);
		General.info("Launched peer 0");
		TestProcess gossip1 = new TestProcess(gossip.Main.class, properties, classpath, peer1);
		TestProcess rps1 = new TestProcess(mockups.rps.Main.class, properties, classpath, peer1);
		General.info("Launched peer 1");
		Thread.sleep(60000);
		RpsApiSocket uut = new RpsApiSocket(new InetSocketAddress("127.0.0.1", 31101));
		General.info("Launched test");
		int id = uut.register();
		for (int i = 0; i < 24; i++) {
			RpsPeerMessage rpm = uut.RPSQUERY(uut.newRpsQueryMessage(id));
			if (rpm == null) {
				General.info("No peer!");
			} else {
				General.info("Received peer with address: " + rpm.getAddress());
			}
			Thread.sleep(5000);
		}
		uut.unregister(id);

		rps1.terminate();
		rps0.terminate();
		gossip1.terminate();
		gossip0.terminate();
	}
}
