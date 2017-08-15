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

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

public class TestFramework {
	public static void runTest(Class<? extends Object> test) {
		System.out.println("Running test " + test.getSimpleName() + ":");
		try {
			LinkedList<String> cmd = new LinkedList<String>();
			cmd.addLast("java");
			cmd.addLast("-cp");
			String classpath = System.getProperty("user.dir") + "/bin/";
			for (String path : Helper.classpath) {
				classpath += ":" + path;
			}
			cmd.addLast(classpath);
			cmd.addLast(test.getName());
			Process p = new ProcessBuilder(cmd).start();
			new RedirectThread(p.getInputStream(), System.out).start();
			new RedirectThread(p.getErrorStream(), System.err).start();
			if (!p.waitFor(2, TimeUnit.MINUTES)) {
				p.destroy();
				if (!p.waitFor(2, TimeUnit.SECONDS)) {
					p.destroyForcibly();
					p.waitFor();
				}
				fail("Test reaches timeout!");
			} else if (p.exitValue() != 0) {
				fail("Test returns non-null!");
			}
		} catch (IOException | InterruptedException e) {
			fail("Test throws unexpected Exception!");
		}
	}

	public static class RedirectThread extends Thread {
		private final InputStream is;
		private final OutputStream os;

		public RedirectThread(InputStream is, OutputStream os) {
			this.is = is;
			this.os = os;
		}

		public void run() {
			byte buffer[] = new byte[1024];
			int n = 0;
			try {
				while ((n = is.read(buffer)) > -1) {
					os.write(buffer, 0, n);
				}
			} catch (IOException e) {
				return;
			}
		}
	}
}
