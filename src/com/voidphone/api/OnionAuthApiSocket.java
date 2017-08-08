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
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.voidphone.general.General;
import com.voidphone.general.IllegalIDException;
import com.voidphone.general.SizeLimitExceededException;
import com.voidphone.onion.Main;

import auth.api.OnionAuthApiMessage;
import auth.api.OnionAuthCipherDecrypt;
import auth.api.OnionAuthCipherDecryptResp;
import auth.api.OnionAuthCipherEncrypt;
import auth.api.OnionAuthCipherEncryptResp;
import auth.api.OnionAuthClose;
import auth.api.OnionAuthDecrypt;
import auth.api.OnionAuthDecryptResp;
import auth.api.OnionAuthEncrypt;
import auth.api.OnionAuthEncryptResp;
import auth.api.OnionAuthSessionHS1;
import auth.api.OnionAuthSessionHS2;
import auth.api.OnionAuthSessionIncomingHS1;
import auth.api.OnionAuthSessionIncomingHS2;
import auth.api.OnionAuthSessionStartMessage;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol.MessageType;
import protocol.ProtocolException;

public class OnionAuthApiSocket extends ApiSocket {
	private final ReentrantReadWriteLock lock;
	private final HashMap<Integer, LinkedBlockingQueue<OnionAuthApiMessage>> map;
	private final Random random;

	public OnionAuthApiSocket(InetSocketAddress addr) throws IOException {
		super(addr);
		random = new Random();
		map = new HashMap<Integer, LinkedBlockingQueue<OnionAuthApiMessage>>();
		lock = new ReentrantReadWriteLock(true);
	}

	public OnionAuthSessionStartMessage newOnionAuthSessionStartMessage(int id, RSAPublicKey key)
			throws IllegalIDException {
		getReadQueue(id);
		try {
			return new OnionAuthSessionStartMessage((long) id, key);
		} catch (MessageSizeExceededException e) {
			General.fatalException(e);
			return null;
		}
	}

	public OnionAuthSessionIncomingHS1 newOnionAuthSessionIncomingHS1(int id, byte payload[])
			throws IllegalIDException {
		getReadQueue(id);
		try {
			return new OnionAuthSessionIncomingHS1((long) id, payload);
		} catch (MessageSizeExceededException e) {
			General.fatalException(e);
			return null;
		}
	}

	public OnionAuthSessionIncomingHS2 newOnionAuthSessionIncomingHS2(int id, int session, byte payload[])
			throws IllegalIDException {
		getReadQueue(id);
		try {
			return new OnionAuthSessionIncomingHS2(session, (long) id, payload);
		} catch (MessageSizeExceededException e) {
			General.fatalException(e);
			return null;
		}
	}

	public OnionAuthEncrypt newOnionAuthEncrypt(int id, int sessions[], byte payload[]) throws IllegalIDException {
		getReadQueue(id);
		try {
			return new OnionAuthEncrypt((long) id, sessions, payload);
		} catch (MessageSizeExceededException e) {
			General.fatalException(e);
			return null;
		}
	}

	public OnionAuthDecrypt newOnionAuthDecrypt(int id, int sessions[], byte payload[]) throws IllegalIDException {
		getReadQueue(id);
		try {
			return new OnionAuthDecrypt((long) id, sessions, payload);
		} catch (MessageSizeExceededException e) {
			General.fatalException(e);
			return null;
		}
	}

	public OnionAuthCipherEncrypt newOnionAuthEncrypt(int id, int session, boolean isCipher, byte payload[])
			throws IllegalIDException {
		getReadQueue(id);
		try {
			return new OnionAuthCipherEncrypt(isCipher, id, session, payload);
		} catch (MessageSizeExceededException e) {
			General.fatalException(e);
			return null;
		}
	}

	public OnionAuthCipherDecrypt newOnionAuthDecrypt(int id, int session, byte payload[]) throws IllegalIDException {
		getReadQueue(id);
		try {
			return new OnionAuthCipherDecrypt(id, session, payload);
		} catch (MessageSizeExceededException e) {
			General.fatalException(e);
			return null;
		}
	}

	public OnionAuthClose newOnionAuthDecrypt(int id) throws IllegalIDException {
		getReadQueue(id);
		return new OnionAuthClose(id);

	}

	/**
	 * Requests OnionAuth to start a new session.
	 * 
	 * @param oassm
	 * @return the first handshake packet
	 * @throws IllegalIDException
	 * @throws InterruptedException
	 */
	public OnionAuthSessionHS1 AUTHSESSIONSTART(OnionAuthSessionStartMessage oassm)
			throws InterruptedException, IllegalIDException {
		connection.sendMsg(oassm);
		return (OnionAuthSessionHS1) getReadQueue((int) oassm.getRequestID()).poll(Main.getConfig().apiTimeout,
				TimeUnit.MILLISECONDS);
	}

	/**
	 * Requests OnionAuth to answer to an handshake request.
	 * 
	 * @param oasihs1
	 * @return the second handshake packet
	 * @throws IllegalIDException
	 * @throws InterruptedException
	 */
	public OnionAuthSessionHS2 AUTHSESSIONINCOMINGHS1(OnionAuthSessionIncomingHS1 oasihs1)
			throws InterruptedException, IllegalIDException {
		connection.sendMsg(oasihs1);
		return (OnionAuthSessionHS2) getReadQueue((int) oasihs1.getRequestID()).poll(Main.getConfig().apiTimeout,
				TimeUnit.MILLISECONDS);
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
	 * @throws IllegalIDException
	 * @throws InterruptedException
	 */
	public OnionAuthEncryptResp AUTHLAYERENCRYPT(OnionAuthEncrypt oalem)
			throws InterruptedException, IllegalIDException {
		connection.sendMsg(oalem);
		return (OnionAuthEncryptResp) getReadQueue((int) oalem.getRequestID()).poll(Main.getConfig().apiTimeout,
				TimeUnit.MILLISECONDS);
	}

	/**
	 * Requests OnionAuth to decrypt a layer-encrypted message.
	 * 
	 * @param oalem
	 * @return
	 * @throws IllegalIDException
	 * @throws InterruptedException
	 */
	public OnionAuthDecryptResp AUTHLAYERDECRYPT(OnionAuthDecrypt oaldm)
			throws InterruptedException, IllegalIDException {
		connection.sendMsg(oaldm);
		return (OnionAuthDecryptResp) getReadQueue((int) oaldm.getRequestID()).poll(Main.getConfig().apiTimeout,
				TimeUnit.MILLISECONDS);
	}

	/**
	 * Requests OnionAuth to encrypt a message.
	 * 
	 * @param oalem
	 * @return
	 * @throws IllegalIDException
	 * @throws InterruptedException
	 */
	public OnionAuthCipherEncryptResp AUTHCIPHERENCRYPT(OnionAuthCipherEncrypt oacem)
			throws InterruptedException, IllegalIDException {
		connection.sendMsg(oacem);
		return (OnionAuthCipherEncryptResp) getReadQueue((int) oacem.getRequestID()).poll(Main.getConfig().apiTimeout,
				TimeUnit.MILLISECONDS);
	}

	/**
	 * Requests OnionAuth to decrypt a message.
	 * 
	 * @param oalem
	 * @return
	 * @throws IllegalIDException
	 * @throws InterruptedException
	 */
	public OnionAuthCipherDecryptResp AUTHCIPHERDECRYPT(OnionAuthCipherDecrypt oacdm)
			throws InterruptedException, IllegalIDException {
		connection.sendMsg(oacdm);
		return (OnionAuthCipherDecryptResp) getReadQueue((int) oacdm.getRequestID()).poll(Main.getConfig().apiTimeout,
				TimeUnit.MILLISECONDS);
	}

	/**
	 * Requests OnionAuth to close a session.
	 * 
	 * @param oalem
	 * @return
	 */
	public void AUTHSESSIONCLOSE(OnionAuthClose oascm) {
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
		OnionAuthApiMessage message;
		int id;
		switch (type) {
		case API_AUTH_SESSION_HS1: {
			OnionAuthSessionHS1 msg = OnionAuthSessionHS1.parse(buffer);
			id = (int) msg.getRequestID();
			message = msg;
			break;
		}
		case API_AUTH_SESSION_HS2: {
			OnionAuthSessionHS2 msg = OnionAuthSessionHS2.parse(buffer);
			id = (int) msg.getRequestID();
			message = msg;
			break;
		}
		case API_AUTH_LAYER_ENCRYPT_RESP: {
			OnionAuthEncryptResp msg = OnionAuthEncryptResp.parse(buffer);
			id = (int) msg.getRequestID();
			message = msg;
			break;
		}
		case API_AUTH_LAYER_DECRYPT_RESP: {
			OnionAuthDecryptResp msg = OnionAuthDecryptResp.parse(buffer);
			id = (int) msg.getRequestID();
			message = msg;
			break;
		}
		case API_AUTH_CIPHER_ENCRYPT_RESP: {
			OnionAuthCipherEncryptResp msg = OnionAuthCipherEncryptResp.parse(buffer);
			id = (int) msg.getRequestID();
			message = msg;
			break;
		}
		case API_AUTH_CIPHER_DECRYPT_RESP: {
			OnionAuthCipherDecryptResp msg = OnionAuthCipherDecryptResp.parse(buffer);
			id = (int) msg.getRequestID();
			message = msg;
			break;
		}
		default:
			throw new ProtocolException("Unexpected message received");
		}
		try {
			getReadQueue(id).offer(message);
		} catch (IllegalIDException e) {
			General.warning("Received illegal ID in OnionAuthApiSocket!");
		}
	}

	private LinkedBlockingQueue<OnionAuthApiMessage> getReadQueue(int id) throws IllegalIDException {
		lock.readLock().lock();
		if (!map.containsKey(id)) {
			lock.readLock().unlock();
			throw new IllegalIDException();
		}
		LinkedBlockingQueue<OnionAuthApiMessage> ret = map.get(id);
		lock.readLock().unlock();
		return ret;
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
		int id;
		lock.writeLock().lock();
		if (map.size() >= Short.MAX_VALUE) {
			lock.writeLock().unlock();
			throw new SizeLimitExceededException("Too many connections registered!");
		}
		do {
			id = random.nextInt();
		} while (map.containsKey(id));
		map.put(id, new LinkedBlockingQueue<OnionAuthApiMessage>(1));
		lock.writeLock().unlock();
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
		lock.writeLock().lock();
		if (!map.containsKey(id)) {
			lock.writeLock().unlock();
			throw new IllegalIDException();
		}
		map.remove(id);
		lock.writeLock().unlock();
	}
}
