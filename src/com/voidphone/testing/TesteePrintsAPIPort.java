package com.voidphone.testing;

import java.io.BufferedReader;

import com.voidphone.general.TestProcess;

public class TesteePrintsAPIPort {
	public static void main(String args[]) throws Exception{
		TestProcess p = new TestProcess(com.voidphone.onion.Main.class, new String[]{"-c", "bla"});
		BufferedReader out = p.getOut();
		Stub.getAPIPort(out);
		p.terminate();
	}
}
