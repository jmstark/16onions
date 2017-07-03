package com.voidphone.general;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class TestFramework {
	public static void runTest(Class<? extends Object> test) {
		System.out.println("Running test " + test.getSimpleName() + ":");
		try {
			Process p = new ProcessBuilder(Arrays.asList(new String[] { "java",
					"-cp", System.getProperty("user.dir") + "/bin/",
					test.getName() })).start();
			new RedirectThread(p.getInputStream(), System.out).start();
			new RedirectThread(p.getErrorStream(), System.err).start();
			if (!p.waitFor(5, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				p.waitFor();
				fail("Test reaches timeout!");
			} else if (p.exitValue() != 0) {
				fail("Test returns non-null!");
			}
		} catch (IOException | InterruptedException e) {
			fail("Test throws unexpected Exception!");
		}
	}

	public static class RedirectThread extends Thread {
		InputStream is;
		OutputStream os;

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
