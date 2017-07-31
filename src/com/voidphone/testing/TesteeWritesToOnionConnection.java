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
import java.net.Socket;

import org.bouncycastle.util.Arrays;

import com.voidphone.testing.Helper.RedirectBackupThread;

public class TesteeWritesToOnionConnection {
	private static RedirectBackupThread rbt;

	public static void main(String args[]) throws Exception {
		Helper.generateConfig(1);
		TestProcess p = new TestProcess(com.voidphone.onion.Main.class, Helper.classpath,
				new String[] { "-c", Helper.getConfigPath(0) });
		rbt = new RedirectBackupThread(p.getOut());
		rbt.start();
		Socket api = Helper.connectToAPI(rbt, Helper.getPeerConfig(0));
		new TestPeer0(rbt, Helper.getPeerConfig(0)).run();
		Thread.sleep(500);
		p.terminate();
		api.close();
	}

	private static class TestPeer0 extends Helper.TestPeer {
		public TestPeer0(RedirectBackupThread rbt, ConfigFactory config) {
			super(rbt, config);
		}

		@Override
		public void run() {
			try {
				writeControl((short) 123, new byte[] { 1, 2, 3 });
				if (!Arrays.areEqual(readControl((short) 123), new byte[] { 4, 5, 6 })) {
					Helper.fatal("Received wrong packet!");
				} else {
					System.out.println("Got 4, 5, 6");
				}
				writeControl((short) 123, new byte[] { 7, 8, 9 });
				if (!Arrays.areEqual(readControl((short) 123), new byte[] { 10, 11, 12 })) {
					Helper.fatal("Received wrong packet!");
				}
			} catch (IOException e) {
				Helper.fatal(e.getMessage());
			}

		}
	}
}
