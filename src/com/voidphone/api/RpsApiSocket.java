/*
 * Copyright (c) 2017, Charlie Groh and Josef Stark. All rights reserved.
 * 
 * This file is part of 16onions.
 *
 * 16onions is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * 16onions is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with 16onions.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.voidphone.api;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.voidphone.general.General;
import com.voidphone.general.IllegalIDException;
import com.voidphone.onion.Main;

import protocol.MessageParserException;
import protocol.Protocol.MessageType;
import protocol.ProtocolException;
import rps.api.RpsPeerMessage;
import rps.api.RpsQueryMessage;

public class RpsApiSocket extends ApiSocket {
	private final LinkedBlockingQueue<RpsPeerMessage> peerQueue;

	/**
	 * Creates a new RPS API connection.
	 * 
	 * @param addr
	 *            the address of the RPS module
	 * @throws IOException
	 *             if there is an I/O-error
	 */
	public RpsApiSocket(InetSocketAddress addr) throws IOException {
		super(addr);
		peerQueue = new LinkedBlockingQueue<RpsPeerMessage>(32);
	}

	/**
	 * Sends a RPSQUERY message to the RPS module to fill the queue with random
	 * peers.
	 */
	private void fillPeerQueue() {
		if (connection == null) {
			return;
		}
		if (peerQueue.remainingCapacity() == 0) {
			General.info("Peer queue is full");
			return;
		}
		connection.sendMsg(new RpsQueryMessage());
		General.info("Sent RPSQUERY message");
	}

	/**
	 * Returns a RpsQueryMessage.
	 * 
	 * @param id
	 *            the ID
	 * @return the RpsQueryMessage
	 * @throws IllegalIDException
	 *             if the ID is not registered
	 */
	public RpsQueryMessage newRpsQueryMessage(int id) throws IllegalIDException {
		if (id != 0) {
			throw new IllegalIDException();
		}
		return new RpsQueryMessage();
	}

	/**
	 * Returns a random peer packed into a RpsPeerMessage. A RpsQueryMessage
	 * (created by newRpsQueryMessage(...)) must be provided by the caller.
	 * 
	 * @param rqm
	 *            the RpsQueryMessage
	 * @return the RpsPeerMessage
	 * @throws InterruptedException
	 *             if the queue is interrupted
	 */
	public RpsPeerMessage RPSQUERY(RpsQueryMessage rqm) throws InterruptedException {
		fillPeerQueue();
		return peerQueue.poll(Main.getConfig().apiTimeout, TimeUnit.MILLISECONDS);
	}

	/**
	 * Parses the received message and places it in the peer queue.
	 * 
	 * @param buffer
	 *            the received message
	 * @param type
	 *            the type of the received message
	 * @throws MessageParserException
	 *             if there is an error while parsing the message
	 * @throws ProtocolException
	 *             if the message does not match the protocol
	 */
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

	/**
	 * Does nothing.
	 * 
	 * @return 0
	 */
	@Override
	public int register() {
		return 0;
	}

	/**
	 * Does nothing.
	 * 
	 * @param id
	 *            must be 0
	 * @throws IllegalIDException
	 *             if id != 0
	 */
	@Override
	public void unregister(int id) throws IllegalIDException {
		if (id != 0) {
			throw new IllegalIDException();
		}
	}
}
