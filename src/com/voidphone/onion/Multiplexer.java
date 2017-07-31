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
package com.voidphone.onion;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.voidphone.general.IllegalAddressException;
import com.voidphone.general.IllegalIDException;
import com.voidphone.general.SizeLimitExceededException;
import com.voidphone.general.Triple;
import com.voidphone.general.General;

/**
 * This is the central database that contains and manages all logical
 * connections going through this hop. When a logical connection was registered,
 * it is possible to read from and write to it. after using a connection
 * unregister should be called.
 */
public class Multiplexer {
	private final ReentrantReadWriteLock firstLock;
	private final HashMap<InetAddress, Triple<ReentrantReadWriteLock, OnionSocket, HashMap<Short, LinkedBlockingQueue<OnionMessage>>>> first;
	private final SecureRandom random;
	private final ByteBuffer writeBuffer;
	private final DatagramChannel channel;

	/**
	 * Creates a new Multiplexer.
	 * 
	 * @param channel
	 *            a datagram channel
	 * @param size
	 *            the size of a encrypted packet
	 */
	public Multiplexer(DatagramChannel channel, int size) {
		random = new SecureRandom();
		first = new HashMap<InetAddress, Triple<ReentrantReadWriteLock, OnionSocket, HashMap<Short, LinkedBlockingQueue<OnionMessage>>>>();
		firstLock = new ReentrantReadWriteLock(true);
		writeBuffer = ByteBuffer.allocate(size + OnionMessage.ONION_HEADER_SIZE);
		this.channel = channel;
	}

	/**
	 * Returns a previously received onion message (i.e. polls one from the read
	 * queue) or null if it timed out. If no message is available, the method blocks
	 * until it times out. The message is received by either the OnionSocket or the
	 * datagram channel.
	 * 
	 * @param id
	 *            the ID of the logical connection
	 * @param addr
	 *            the address of the endpoint
	 * @return a previously received onion message of null if it timed out
	 * @throws IllegalAddressException
	 *             if the ID is not registerd
	 * @throws IllegalIDException
	 *             if the address is not registered
	 * @throws InterruptedException
	 *             if the operation was interrupted
	 */
	public OnionMessage read(short id, InetAddress addr)
			throws IllegalAddressException, IllegalIDException, InterruptedException {
		return getReadQueue(id, addr).poll(Main.getConfig().onionTimeout, TimeUnit.MILLISECONDS);
	}

	/**
	 * Sends a message through the OnionSocket. This method blocks until the write
	 * completes. For details see send(OnionMessage message) in OnionSocket
	 * 
	 * @param message
	 *            the message to send
	 * @throws IllegalAddressException
	 *             if the address is not registered
	 * @throws InterruptedException
	 *             if the operation was interrupted
	 */
	public void writeControl(OnionMessage message) throws IllegalAddressException, InterruptedException {
		getOnionSocket(message.address).send(message);
	}

	/**
	 * Sends a message through the datagram channel.
	 * 
	 * @param message
	 *            the message to send
	 */
	public void writeData(OnionMessage message) {

		message.serialize(writeBuffer);
		try {
			channel.write(writeBuffer);
		} catch (IOException e) {
			General.fatalException(e);
		}
	}

	private OnionSocket getOnionSocket(InetAddress addr) throws IllegalAddressException {
		return getFirst(addr).b;
	}

	private Triple<ReentrantReadWriteLock, OnionSocket, HashMap<Short, LinkedBlockingQueue<OnionMessage>>> getFirst(
			InetAddress addr) throws IllegalAddressException {
		firstLock.readLock().lock();
		if (!first.containsKey(addr)) {
			firstLock.readLock().unlock();
			throw new IllegalAddressException();
		}
		Triple<ReentrantReadWriteLock, OnionSocket, HashMap<Short, LinkedBlockingQueue<OnionMessage>>> ret = first
				.get(addr);
		firstLock.readLock().unlock();
		return ret;
	}

	private LinkedBlockingQueue<OnionMessage> getSecond(
			Triple<ReentrantReadWriteLock, OnionSocket, HashMap<Short, LinkedBlockingQueue<OnionMessage>>> triple,
			Short id) throws IllegalIDException {
		triple.a.readLock().lock();
		if (!triple.c.containsKey(id)) {
			triple.a.readLock().unlock();
			throw new IllegalIDException();
		}
		LinkedBlockingQueue<OnionMessage> ret = triple.c.get(id);
		triple.a.readLock().unlock();
		return ret;
	}

	/**
	 * Returns the read queue of a logical connection.
	 * 
	 * @param id
	 *            the ID of the logical connection
	 * @param addr
	 *            the address of the endpoint
	 * @return the read queue of a logical connection
	 * @throws IllegalAddressException
	 *             if the address is not registered
	 * @throws IllegalIDException
	 *             if the ID is not registered
	 */
	public LinkedBlockingQueue<OnionMessage> getReadQueue(short id, InetAddress addr)
			throws IllegalAddressException, IllegalIDException {
		return getSecond(getFirst(addr), id);
	}

	/**
	 * Registers a new underlying connection.
	 * 
	 * @param addr
	 *            the address of the endpoint of the connection
	 * @param sock
	 *            an OnionSocket representing the connected to the endpoint
	 * @throws IllegalAddressException
	 *             if the address is already registered
	 */
	public void register(InetAddress addr, OnionSocket sock) throws IllegalAddressException {
		firstLock.writeLock().lock();
		if (first.containsKey(addr)) {
			firstLock.writeLock().unlock();
			throw new IllegalAddressException();
		}
		first.put(addr,
				new Triple<ReentrantReadWriteLock, OnionSocket, HashMap<Short, LinkedBlockingQueue<OnionMessage>>>(
						new ReentrantReadWriteLock(), sock, new HashMap<Short, LinkedBlockingQueue<OnionMessage>>()));
		firstLock.writeLock().unlock();
	}

	/**
	 * Registers a new logical connection.
	 * 
	 * @param addr
	 *            the address of the endpoint
	 * @return the ID of the connection
	 * @throws SizeLimitExceededException
	 *             if too many connections are registered
	 * @throws IllegalAddressException
	 *             if the address is not registered
	 */
	public short register(InetAddress addr) throws SizeLimitExceededException, IllegalAddressException {
		short id;
		HashMap<Short, LinkedBlockingQueue<OnionMessage>> second;
		Triple<ReentrantReadWriteLock, OnionSocket, HashMap<Short, LinkedBlockingQueue<OnionMessage>>> triple;
		triple = getFirst(addr);
		triple.a.writeLock().lock();
		second = triple.c;
		if (second.size() >= Short.MAX_VALUE) {
			triple.a.writeLock().unlock();
			throw new SizeLimitExceededException("Too many connections registered!");
		}
		do {
			id = (short) random.nextInt();
		} while (second.containsKey(id));
		second.put(id, new LinkedBlockingQueue<OnionMessage>());
		triple.a.writeLock().unlock();
		return id;
	}

	/**
	 * Registers a new logical connection with chosen ID.
	 * 
	 * @param id
	 *            the ID of the connection
	 * @param addr
	 *            the address of the endpoint
	 * @throws SizeLimitExceededException
	 *             if too many connections are registered
	 * @throws IllegalIDException
	 *             if the ID is already registered
	 * @throws IllegalAddressException
	 *             if the address is not registered
	 */
	public void register(short id, InetAddress addr)
			throws SizeLimitExceededException, IllegalIDException, IllegalAddressException {
		HashMap<Short, LinkedBlockingQueue<OnionMessage>> second;
		Triple<ReentrantReadWriteLock, OnionSocket, HashMap<Short, LinkedBlockingQueue<OnionMessage>>> triple;
		triple = getFirst(addr);
		triple.a.writeLock().lock();
		second = triple.c;
		if (second.size() >= Short.MAX_VALUE) {
			triple.a.writeLock().unlock();
			throw new SizeLimitExceededException("Too many connections registered!");
		}
		if (second.containsKey(id)) {
			triple.a.writeLock().unlock();
			throw new IllegalIDException();
		}
		second.put(id, new LinkedBlockingQueue<OnionMessage>());
		triple.a.writeLock().unlock();
	}

	/**
	 * Unregisters a underlying connection.
	 * 
	 * @param addr
	 *            the address of the endpoint
	 * @throws IllegalAddressException
	 *             if the address is not registered
	 */
	public void unregister(InetAddress addr) throws IllegalAddressException {
		firstLock.writeLock().lock();
		if (!first.containsKey(addr)) {
			firstLock.writeLock().unlock();
			throw new IllegalAddressException();
		}
		first.remove(addr);
		firstLock.writeLock().unlock();
	}

	/**
	 * Unregisters a logical connection.
	 * 
	 * @param id
	 *            the ID of the connection
	 * @param addr
	 *            the address of the endpoint
	 * @throws IllegalAddressException
	 *             if the address is not registered
	 * @throws IllegalIDException
	 *             if the ID is not registered
	 */
	public void unregister(short id, InetAddress addr) throws IllegalAddressException, IllegalIDException {
		HashMap<Short, LinkedBlockingQueue<OnionMessage>> second;
		Triple<ReentrantReadWriteLock, OnionSocket, HashMap<Short, LinkedBlockingQueue<OnionMessage>>> triple;
		triple = getFirst(addr);
		triple.a.writeLock().lock();
		second = triple.c;
		if (!second.containsKey(id)) {
			triple.a.writeLock().unlock();
			throw new IllegalIDException();
		}
		second.remove(id);
		if (second.isEmpty()) {
			triple.a.writeLock().unlock();
			triple.b.close();
		} else {
			triple.a.writeLock().unlock();
		}
	}
}
