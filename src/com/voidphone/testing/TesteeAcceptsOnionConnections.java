package com.voidphone.testing;

import java.net.Socket;

import com.voidphone.general.TestProcess;

public class TesteeAcceptsOnionConnections {
	public static void main(String args[]) throws Exception{
		TestProcess p = new TestProcess(com.voidphone.onion.Main.class, new String[]{"-c", "bla"});
		Socket api = Stub.getAPISocket(p.getOut());
		int port = Stub.getOnionPort(p.getOut());
		Socket onion = Stub.getOnionSocket(p.getOut(), port);
		onion.close();
		api.close();
		p.terminate();
	}
}
