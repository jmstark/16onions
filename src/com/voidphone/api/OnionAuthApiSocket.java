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
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

import javax.naming.SizeLimitExceededException;

import com.voidphone.general.General;
import com.voidphone.general.IllegalIDException;

import auth.api.OnionAuthApiMessage;
import auth.api.OnionAuthSessionHS1;
import auth.api.OnionAuthSessionHS2;
import auth.api.OnionAuthSessionIncomingHS1;
import auth.api.OnionAuthSessionIncomingHS2;
import auth.api.OnionAuthSessionStartMessage;
import protocol.MessageParserException;
import protocol.Protocol.MessageType;
import protocol.ProtocolException;

public class OnionAuthApiSocket extends ApiSocket {
	private final HashMap<Short, LinkedBlockingQueue<OnionAuthApiMessage>> map;
	private final Random random;

	public OnionAuthApiSocket(InetSocketAddress addr) throws IOException {
		super(addr);
		random = new Random();
		map = new HashMap<Short, LinkedBlockingQueue<OnionAuthApiMessage>>();
	}

	public OnionAuthSessionStartMessage newOnionAuthSessionStartMessage() {

	}

	public OnionAuthSessionIncomingHS1 newOnionAuthSessionIncomingHS1() {

	}

	/**
	 * Requests OnionAuth to start a new session.
	 * 
	 * @param oassm
	 * @return the first handshake packet
	 */
	public OnionAuthSessionHS1 AUTHSESSIONSTART(OnionAuthSessionStartMessage oassm) {

	}

	/**
	 * Requests OnionAuth to answer to an handshake request.
	 * 
	 * @param oasihs1
	 * @return the second handshake packet
	 */
	public OnionAuthSessionHS2 AUTHSESSIONINCOMINGHS1(OnionAuthSessionIncomingHS1 oasihs1) {

	}

	/**
	 * Requests OnionAuth to verify the handshake answer.
	 * 
	 * @param oasihs2
	 */
	public void AUTHSESSIONINCOMINGHS2(OnionAuthSessionIncomingHS2 oasihs2) {
		connection.sendMsg(oasihs2);
	}

	/**
	 * Requests OnionAuth to layer-encrypt a message.
	 * 
	 * @param oalem
	 * @return
	 */
	public OnionAuthLayerEncryptRespMessage AUTHLAYERENCRYPT(OnionAuthLayerEncryptMessage oalem) {

	}

	/**
	 * Requests OnionAuth to decrypt a layer-encrypted message.
	 * 
	 * @param oalem
	 * @return
	 */
	public OnionAuthLayerDecryptRespMessage AUTHLAYERDECRYPT(OnionAuthLayerDecryptMessage oaldm) {

	}

	/**
	 * Requests OnionAuth to encrypt a message.
	 * 
	 * @param oalem
	 * @return
	 */
	public OnionAuthCipherEncryptRespMessage AUTHCIPHERENCRYPT(OnionAuthCipherEncryptMessage oacem) {

	}

	/**
	 * Requests OnionAuth to decrypt a message.
	 * 
	 * @param oalem
	 * @return
	 */
	public OnionAuthCipherDecryptRespMessage AUTHCIPHERDECRYPT(OnionAuthCipherDecryptMessage oacdm) {

	}

	/**
	 * Requests OnionAuth to close a session.
	 * 
	 * @param oalem
	 * @return
	 */
	public void AUTHSESSIONCLOSE(OnionAuthSessionCloseMessage oascm) {
		connection.sendMsg(oascm);
	}

	/**
	 * 
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
		case API_AUTH_SESSION_HS1:
			return;
		default:
			throw new ProtocolException("Unexpected message received");
		}
	}

	/**
	 * Registers a new logical connection to the OnionAuth module.
	 * 
	 * @return ID of the new connection
	 * @throws SizeLimitExceededException
	 *             if too many connections are registered
	 */
	@Override
	public int register() throws SizeLimitExceededException {
		short id;

		if (map.size() >= Short.MAX_VALUE) {
			throw new SizeLimitExceededException("Too many connections registered!");
		}
		do {
			id = (short) random.nextInt();
		} while (map.containsKey(id));
		map.put(id, new LinkedBlockingQueue<OnionAuthApiMessage>(1));
		return (int) id;
	}

	/**
	 * Unregisters a logical connection.
	 * 
	 * @param id
	 *            ID of the connection
	 * @throws IllegalIDException
	 *             if the ID is not registered
	 */
	@Override
	public void unregister(int id) throws IllegalIDException {
		if (!map.containsKey((short) id)) {
			throw new IllegalIDException();
		}
		map.remove((short) id);
	}
}
