package com.voidphone.testing;

import java.net.Socket;

import com.voidphone.testing.TestProcess;
import com.voidphone.testing.Helper.RedirectBackupThread;

public class TesteePrintsOnionPort {
	private static RedirectBackupThread rbt;

	public static void main(String args[]) throws Exception {
		Helper.generateConfig(1);
		TestProcess p = new TestProcess(com.voidphone.onion.Main.class, Helper.classpath,
				new String[] { "-c", Helper.getConfigPath(0) });
		rbt = new RedirectBackupThread(p.getOut());
		rbt.start();
		Socket api = Helper.connectToAPI(rbt, Helper.getPeerConfig(0));
		while (!rbt.contains("Waiting for Onion connections on ")) {
			Thread.sleep(500);
		}
		Thread.sleep(500);
		p.terminate();
		api.close();
	}
}
