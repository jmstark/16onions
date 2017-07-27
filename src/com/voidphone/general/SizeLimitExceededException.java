package com.voidphone.general;

public class SizeLimitExceededException extends Exception {
	private static final long serialVersionUID = 4728355616743993401L;

	public SizeLimitExceededException(String msg) {
		super(msg);
	}
}
