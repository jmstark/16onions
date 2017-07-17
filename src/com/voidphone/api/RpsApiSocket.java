package com.voidphone.api;

import java.io.IOException;
import java.net.InetSocketAddress;

import protocol.MessageParserException;
import rps.api.RpsPeerMessage;
import rps.api.RpsQueryMessage;

public class RpsApiSocket extends ApiSocket {
	public RpsApiSocket(InetSocketAddress addr) throws IOException {
		super(addr);
	}

	/**
	 * Sends a RPS QUERY message to the RPS module, waits for the answer and
	 * parses it.
	 * 
	 * @return a random peer, represented by a RPSPEER-object
	 * @throws IOException
	 *             if an I/O error occurs
	 * @throws MessageParserException
	 *             if a wrong message is received
	 */
	public RpsPeerMessage RPSQUERY() throws IOException, MessageParserException {
		new RpsQueryMessage().send(writeBuffer);
		writeBuffer.flip();
		channel.write(writeBuffer);
		channel.read(readBuffer);
		return RpsPeerMessage.parse(readBuffer);
	}
}
