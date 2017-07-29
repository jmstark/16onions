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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import com.voidphone.general.General;

public class TestProcess {
	private Process process;

	public TestProcess(Class<? extends Object> main, String args[]) throws IOException {
		this(main, new HashMap<String, String>(), new String[] {}, args);
	}

	public TestProcess(Class<? extends Object> main, String classpath[], String args[]) throws IOException {
		this(main, new HashMap<String, String>(), classpath, args);
	}

	public TestProcess(Class<? extends Object> main, Map<String, String> properties, String classpath[], String args[])
			throws IOException {
		int i = 0;
		String cmd[] = new String[args.length + properties.size() + 4];
		cmd[i++] = "java";
		for (Entry<String, String> property : properties.entrySet()) {
			cmd[i++] = "-D" + property.getKey() + "=" + property.getValue();
		}
		cmd[i++] = "-cp";
		String cp = System.getProperty("user.dir") + "/bin/";
		for (String library : classpath) {
			cp += ":" + library;
		}
		cmd[i++] = cp;
		cmd[i++] = main.getName();
		System.arraycopy(args, 0, cmd, i, args.length);
		process = new ProcessBuilder(Arrays.asList(cmd)).start();
		new TestFramework.RedirectThread(process.getErrorStream(), System.err).start();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					terminate();
				} catch (InterruptedException e) {
					General.error("Could not terminate process!");
				}
			}
		});
	}

	public BufferedReader getOut() {
		if (process.isAlive()) {
			return new BufferedReader(new InputStreamReader(process.getInputStream()));
		} else {
			return null;
		}
	}

	public void terminate() throws InterruptedException {
		process.destroy();
		if (!process.waitFor(1, TimeUnit.SECONDS)) {
			process.destroyForcibly();
			process.waitFor();
		}
	}
}
