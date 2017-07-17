package com.voidphone.api.rps;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.voidphone.api.APISocket;

public class RPSAPISocket extends APISocket {
	public RPSAPISocket(InetSocketAddress addr) throws IOException {
		super(addr);
	}

	/**
	 * Sends a RPS QUERY message to the RPS module, waits for the answer and
	 * parses it.
	 * 
	 * @return a random peer, represented by a RPSPEER-object
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public RpsPeerMessage RPSQUERY() throws IOException {
		return super.<RpsQueryMessage,RpsPeerMessage>sendrecv(new RpsQueryMessage());
	}
}
