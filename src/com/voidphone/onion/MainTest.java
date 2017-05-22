package com.voidphone.onion;

import static org.junit.Assert.*;

import org.junit.Test;

public class MainTest {

	@Test
	public void testOne() {
		if(Main.one()!=1)
			fail("Returns wrong value. " +
					"Function should always return 1. " +
					"Needs to be fixed ASAP.");
	}

}
