package com.voidphone.testing;

import java.net.Socket;

import com.voidphone.testing.TestProcess;

public class TesteePrintsOnionPort {
	public static void main(String args[]) throws Exception {
		Helper.generateConfig(1);
		TestProcess p = new TestProcess(com.voidphone.onion.Main.class, Helper.classpath,
				new String[] { "-c", Helper.getConfigPath(0) });
		Socket api = new Socket();
		Helper.contains(p.getOut(), "Waiting for API connection on ");
		api.connect(Helper.getAddressFromConfig(Helper.getPeerConfig(0), "onion", "api_address"));
		Helper.contains(p.getOut(), "Waiting for Onion connections on ");
		p.terminate();
		api.close();
	}
}
