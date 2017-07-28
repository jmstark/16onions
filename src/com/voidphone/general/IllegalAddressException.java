package com.voidphone.general;

public class IllegalAddressException extends Exception {
	private static final long serialVersionUID = -3790776669504352693L;

	public IllegalAddressException() {
		super("Illegal address!");
	}
}
