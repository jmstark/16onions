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
import java.util.Arrays;

import com.voidphone.testing.Helper.RedirectBackupThread;

public class TesteeManagesMultipleOnionConnections {
	private static RedirectBackupThread rbt;

	public static void main(String args[]) throws Exception {
		Helper.generateConfig(1);
		TestProcess p = new TestProcess(com.voidphone.onion.Main.class, Helper.classpath,
				new String[] { "-c", Helper.getConfigPath(0) });
		rbt = new RedirectBackupThread(p.getOut());
		rbt.start();
		Socket api = Helper.connectToAPI(rbt, Helper.getPeerConfig(0));
		TestPeer0 peer0 = new TestPeer0(rbt, Helper.getPeerConfig(0), (short) 123);
		peer0.start();
		TestPeer0 peer1 = new TestPeer0(rbt, Helper.getPeerConfig(0), (short) 7);
		peer1.start();
		peer0.join();
		peer1.join();
		Thread.sleep(100);
		p.terminate();
		api.close();
	}

	private static class TestPeer0 extends Helper.TestPeer {
		short id;

		public TestPeer0(RedirectBackupThread rbt, ConfigFactory config, short id) {
			super(rbt, config);
			this.id = id;
		}

		@Override
		public void run() {
			Helper.info("Started " + id);
			try {
				for (int i = 0; i < 20; i++) {
					writeControl(id, new byte[] { 1, 2, -1 });
					if (!Arrays.equals(readControl((short) 123), new byte[] { 3, 4, -1 })) {
						Helper.fatal("Received wrong packet!");
					} else {
						Helper.info(id + ": Got " + Arrays.toString(new byte[] { 3, 4, -1 }));
					}
				}
			} catch (IOException e) {
				Helper.fatal(e.getMessage());
			}

		}
	}
}
