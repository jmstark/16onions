package com.voidphone.general;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class TestProcess {
	private Process process;

	public TestProcess(Class<? extends Object> main, String args[])
			throws IOException {
		String cmd[] = new String[args.length + 4];
		cmd[0] = "java";
		cmd[1] = "-cp";
		cmd[2] = System.getProperty("user.dir") + "/bin/";
		cmd[3] = main.getName();
		System.arraycopy(args, 0, cmd, 4, args.length);
		process = new ProcessBuilder(Arrays.asList(cmd)).start();
		new TestFramework.RedirectThread(process.getErrorStream(), System.err)
				.start();
	}

	public BufferedReader getOut() {
		if (process.isAlive()) {
			return new BufferedReader(new InputStreamReader(
					process.getInputStream()));
		} else {
			return null;
		}
	}

//	public BufferedReader getErr() {
//		return new BufferedReader(new InputStreamReader(
//				process.getErrorStream()));
//	}

//	public BufferedWriter getIn() {
//		return new BufferedWriter(new OutputStreamWriter(
//				process.getOutputStream()));
//	}

	public void terminate() throws InterruptedException {
		process.destroy();
		if (!process.waitFor(1, TimeUnit.SECONDS)) {
			process.destroyForcibly();
			process.waitFor();
		}
	}
}
