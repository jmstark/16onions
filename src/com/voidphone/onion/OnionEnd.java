package com.voidphone.onion;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.voidphone.api.Config;

public class OnionEnd extends OnionBase {
	public OnionEnd(InetSocketAddress addr, Config c) throws IOException {
		super(c);
		connect(addr, c.getHostkey());
	}
	
	public void send() {
		
	}
	
	public byte[] recv() {
		return null;
	}
}
