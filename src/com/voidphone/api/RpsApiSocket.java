package com.voidphone.api;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.voidphone.general.General;

import protocol.MessageParserException;
import protocol.Protocol.MessageType;
import protocol.ProtocolException;
import rps.api.RpsPeerMessage;
import rps.api.RpsQueryMessage;

public class RpsApiSocket extends ApiSocket {
	private final LinkedBlockingQueue<RpsPeerMessage> peerQueue;

	public RpsApiSocket(InetSocketAddress addr) throws IOException {
		super(addr);
		peerQueue = new LinkedBlockingQueue<RpsPeerMessage>(32);
	}

	private void fillPeerQueue() {
		if (peerQueue.remainingCapacity() == 0 || connection == null) {
			return;
		}
		connection.sendMsg(new RpsQueryMessage());
		General.info("Sent RPSQUERY message");
	}

	/**
	 * Sends a RPS QUERY message to the RPS module, waits for the answer and parses
	 * it.
	 * 
	 * @return the RpsPeerMessage
	 * @throws InterruptedException
	 *             if the queue is interrupted
	 */
	public RpsPeerMessage RPSQUERY() throws InterruptedException {
		fillPeerQueue();
		return peerQueue.poll(1, TimeUnit.SECONDS);
	}

	@Override
	protected void receive(ByteBuffer buffer, MessageType type) throws MessageParserException, ProtocolException {
		switch (type) {
		case API_RPS_PEER:
			General.info("Received RPSPEER message");
			fillPeerQueue();
			peerQueue.offer(RpsPeerMessage.parse(buffer));
			return;
		default:
			throw new ProtocolException("Unexpected message received");
		}
	}
}
