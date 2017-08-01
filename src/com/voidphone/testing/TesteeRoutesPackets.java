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
import java.nio.ByteBuffer;
import java.util.Arrays;

import com.voidphone.testing.Helper.RedirectBackupThread;

public class TesteeRoutesPackets {
	private static RedirectBackupThread rbt1;
	private static RedirectBackupThread rbt2;

	public static void main(String args[]) throws Exception {
		Helper.generateConfig(2);
		TestProcess p1 = new TestProcess(com.voidphone.onion.Main.class, Helper.classpath,
				new String[] { "-c", Helper.getConfigPath(0) });
		TestProcess p2 = new TestProcess(com.voidphone.onion.Main.class, Helper.classpath,
				new String[] { "-c", Helper.getConfigPath(1) });
		rbt1 = new RedirectBackupThread(p1.getOut(), 0);
		rbt1.start();
		rbt2 = new RedirectBackupThread(p2.getOut(), 1);
		rbt2.start();
		Socket api1 = Helper.connectToAPI(rbt1, Helper.getPeerConfig(0));
		Socket api2 = Helper.connectToAPI(rbt2, Helper.getPeerConfig(1));
		TestPeer0 peer0 = new TestPeer0(rbt1, Helper.getPeerConfig(0), (short) 123);
		peer0.start();
		TestPeer0 peer1 = new TestPeer0(rbt1, Helper.getPeerConfig(0), (short) 7);
		peer1.start();
		peer0.join();
		peer1.join();
		rbt2.contains(Arrays.toString(new byte[] { 19, 7, 5 }));
		rbt2.contains(Arrays.toString(new byte[] { 0, 0, 0 }));
		Thread.sleep(100);
		p1.terminate();
		p2.terminate();
		api1.close();
		api2.close();
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
				byte buf[] = new byte[] { 0, 0, 0 };
				ByteBuffer bbuf = ByteBuffer.wrap(buf);
				short nextPort = (short) Helper.getPeerConfig(1).config.get("onion", "p2p_port", Integer.class)
						.intValue();
				bbuf.putShort(nextPort);
				writeControl(id, buf);
				writeControl(id, new byte[] { 0, 0, 1 });
				for (int i = 0; i < 20; i++) {
					writeControl(id, new byte[] { (byte) i, (byte) id, 5 });
				}
				writeControl(id, new byte[] { 0, 0, 0 });
				getSocket().close();
			} catch (IOException e) {
				Helper.fatal(e.getMessage());
			}
		}
	}
}
