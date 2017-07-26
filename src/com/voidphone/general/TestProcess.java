package com.voidphone.general;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

public class TestProcess {
	private Process process;

	public TestProcess(Class<? extends Object> main, String args[]) throws IOException {
		this(main, new HashMap<String,String>(), new String[]{}, args);
	}
	
	public TestProcess(Class<? extends Object> main, Map<String,String> properties, String classpath[], String args[]) throws IOException {
		int i = 0;
		String cmd[] = new String[args.length + properties.size() + 4];
		cmd[i++] = "java";
		for (Entry<String,String> property : properties.entrySet()) {
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
