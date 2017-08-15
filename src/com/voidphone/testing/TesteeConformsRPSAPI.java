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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.HashMap;
import java.util.concurrent.Executors;

import com.voidphone.testing.TestProcess;

import rps.api.RpsPeerMessage;

import com.voidphone.api.RpsApiSocket;
import com.voidphone.testing.Helper.RedirectBackupThread;

public class TesteeConformsRPSAPI {
	public static void main(String args[]) throws Exception {
		Helper.generateConfig(3, 0);
		newPeer(0);
		newPeer(1);
		newPeer(2);

		Thread.sleep(60000);

		Helper.info("Launching test!");
		String addr = Helper.getPeerConfig(0).config.get("rps", "api_address", String.class);
		InetSocketAddress address = new InetSocketAddress(addr.substring(0, addr.lastIndexOf(":")),
				Integer.parseInt(addr.substring(addr.lastIndexOf(":") + 1)));
		RpsApiSocket ras = new RpsApiSocket(address,
				AsynchronousChannelGroup.withFixedThreadPool(1, Executors.defaultThreadFactory()), 5000);
		int id = ras.register();
		int responseCounter = 0;
		int queryCounter = 0;
		for (; queryCounter <= 16;) {
			RpsPeerMessage rpm = ras.RPSQUERY(ras.newRpsQueryMessage(id));
			queryCounter++;
			if (rpm == null) {
				Helper.info("No peer!");
			} else {
				Helper.info("Received peer with address: " + rpm.getAddress());
				responseCounter++;
			}
			Thread.sleep(100);
		}
		ras.unregister(id);
		Helper.info("Sent " + queryCounter + " queries. Got " + responseCounter + " responses.");
		if (responseCounter < queryCounter / 2) {
			System.exit(1);
		} else {
			System.exit(0);
		}
	}

	public static void newPeer(int i) throws IOException, InterruptedException {
		HashMap<String, String> properties = new HashMap<String, String>();
		properties.put("keystore.config.file", System.getProperty("keystore.config.file", "security.properties"));
		RedirectBackupThread rbt[] = new RedirectBackupThread[2];
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
		Helper.info("Launched peer " + i);
	}
}
