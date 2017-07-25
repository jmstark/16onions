package com.voidphone.api;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import protocol.MessageHandler;
import protocol.MessageParserException;
import protocol.Protocol.MessageType;
import protocol.ProtocolException;
import rps.api.RpsApiMessage;
import rps.api.RpsPeerMessage;
import rps.api.RpsQueryMessage;

public class RpsApiSocket extends ApiSocket {
	private RpsMessageHandler handler;

	public RpsApiSocket(InetSocketAddress addr) throws IOException {
		super(addr);
		handler = new RpsMessageHandler(null);
	}

	/**
	 * Sends a RPS QUERY message to the RPS module, waits for the answer and parses
	 * it.
	 * 
	 * @return a random peer, represented by a RPSPEER-object
	 * @throws IOException
	 *             if an I/O error occurs
	 * @throws MessageParserException
	 * @throws ProtocolException
	 *             if a wrong message is received
	 */
	public RpsPeerMessage RPSQUERY() throws IOException, MessageParserException, ProtocolException {
		new RpsQueryMessage().send(writeBuffer);
		writeBuffer.flip();
		channel.write(writeBuffer);
		channel.read(readBuffer);
		handler.parseMessage(readBuffer);
		return (RpsPeerMessage)handler.getMessage();
	}

	private class RpsMessageHandler extends MessageHandler<Void> {
		private RpsApiMessage message;

		private RpsMessageHandler(Void closure) {
			super(closure);
		}

		@Override
		public void parseMessage(ByteBuffer buf, MessageType type, Void closure)
				throws MessageParserException, ProtocolException {
			switch (type) {
			case API_RPS_PEER:
				message = RpsPeerMessage.parse(buf);
				return;
			default:
				throw new ProtocolException("Unexpected message received");
			}
		}

		public RpsApiMessage getMessage() {
			return message;
		}
	}
}
