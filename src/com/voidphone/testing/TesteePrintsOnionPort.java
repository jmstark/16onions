package com.voidphone.testing;

import java.net.Socket;

import com.voidphone.general.TestProcess;

public class TesteePrintsOnionPort {
	public static void main(String args[]) throws Exception {
		TestProcess p = new TestProcess(com.voidphone.onion.Main.class, new String[]{"-c", "bla"});
		Socket api = Stub.getAPISocket(p.getOut());
		Stub.getOnionPort(p.getOut());
		api.close();
		p.terminate();
	}
}
