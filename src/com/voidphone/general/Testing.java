package com.voidphone.general;

import org.junit.Test;

public class Testing {
	@Test
	public void testeePrintsAPIPort() {
		TestFramework.runTest(com.voidphone.testing.TesteePrintsAPIPort.class);
	}
	
	@Test
	public void testeePrintsOnionPort() {
		TestFramework.runTest(com.voidphone.testing.TesteePrintsOnionPort.class);
	}
	
	@Test
	public void testeeAcceptsOnionConnection() {
		TestFramework.runTest(com.voidphone.testing.TesteeAcceptsOnionConnections.class);
	}
}
