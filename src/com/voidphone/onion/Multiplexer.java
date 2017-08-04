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
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
	private final HashMap<InetSocketAddress, Triple<ReentrantReadWriteLock, OnionSocket, HashMap<Short, LinkedBlockingQueue<OnionMessage>>>> first;
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
		first = new HashMap<InetSocketAddress, Triple<ReentrantReadWriteLock, OnionSocket, HashMap<Short, LinkedBlockingQueue<OnionMessage>>>>();
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
	public OnionMessage read(short id, InetSocketAddress addr)
			throws IllegalAddressException, IllegalIDException, InterruptedException {
		return getReadQueue(id, addr).poll(Main.getConfig().onionTimeout, TimeUnit.MILLISECONDS);
	}

	/**
	 * Sends a message. This method blocks until the write completes.
	 * 
	 * @param message
	 *            the message to send
	 * @throws IllegalAddressException
	 *             if the address is not registered
	 * @throws InterruptedException
	 *             if the operation was interrupted
	 * @throws IOException
	 *             if an I/O-error occurs
	 */
	public void write(OnionMessage message) throws IllegalAddressException, InterruptedException, IOException {
		if (message.type == OnionMessage.CONTROL_MESSAGE) {
			getOnionSocket(message.address).send(message);
		} else {
			message.serialize(writeBuffer);
			try {
				channel.write(writeBuffer);
			} catch (IOException e) {
				General.fatalException(e);
			}
		}

	}

	/**
	 * Merges to logical connections for reading.
	 * 
	 * @param id1
	 *            id of logical connection 1
	 * @param addr1
	 *            address of logical connection 1
	 * @param id2
	 *            id of logical connection 2
	 * @param addr2
	 *            address of logical connection 2
	 * @throws IllegalAddressException
	 *             if either addr1 or addr2, is not registered
	 * @throws IllegalIDException
	 *             if either id1 or id2, is not registered
	 */
	public void merge(short id1, InetSocketAddress addr1, short id2, InetSocketAddress addr2)
			throws IllegalAddressException, IllegalIDException {
		LinkedBlockingQueue<OnionMessage> queue1;
		LinkedBlockingQueue<OnionMessage> queue2;
		Triple<ReentrantReadWriteLock, OnionSocket, HashMap<Short, LinkedBlockingQueue<OnionMessage>>> triple1;
		triple1 = getFirst(addr1);
		queue1 = getReadQueue(id1, addr1);
		queue2 = getReadQueue(id2, addr2);
		triple1.a.writeLock().lock();
		if (!triple1.c.containsKey(id1)) {
			triple1.a.writeLock().unlock();
			throw new IllegalIDException();
		}
		queue2.addAll(queue1);
		triple1.c.put(id1, queue2);
		triple1.a.writeLock().unlock();
	}

	private OnionSocket getOnionSocket(InetSocketAddress addr) throws IllegalAddressException {
		return getFirst(addr).b;
	}

	private Triple<ReentrantReadWriteLock, OnionSocket, HashMap<Short, LinkedBlockingQueue<OnionMessage>>> getFirst(
			InetSocketAddress addr) throws IllegalAddressException {
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
	public LinkedBlockingQueue<OnionMessage> getReadQueue(short id, InetSocketAddress addr)
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
	public void registerAddress(InetSocketAddress addr, OnionSocket sock) throws IllegalAddressException {
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
	 * Creates a new underlying connection and registers it.
	 * 
	 * @param addr
	 *            the address of the endpoint of the connection
	 * @throws InterruptedException
	 *             if the connection process is interrupted
	 * @throws TimeoutException
	 *             if the connection process timed out
	 * @throws IOException
	 *             if an I/O-error occurs
	 * @return the OnionSocket for the address
	 */
	public OnionSocket registerAddress(InetSocketAddress addr)
			throws IOException, TimeoutException, InterruptedException {
		OnionSocket sock = null;
		firstLock.writeLock().lock();
		try {
			if (first.containsKey(addr)) {
				sock = getFirst(addr).b;
				firstLock.writeLock().unlock();
				return sock;
			}
			sock = new OnionSocket(this, addr);
		} catch (IllegalAddressException e) {
			General.fatal("Address is not registered, but it must be!");
		}
		first.put(addr,
				new Triple<ReentrantReadWriteLock, OnionSocket, HashMap<Short, LinkedBlockingQueue<OnionMessage>>>(
						new ReentrantReadWriteLock(), sock, new HashMap<Short, LinkedBlockingQueue<OnionMessage>>()));
		firstLock.writeLock().unlock();
		return sock;

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
	public short registerID(InetSocketAddress addr) throws SizeLimitExceededException, IllegalAddressException {
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
	public void registerID(short id, InetSocketAddress addr)
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
	public void unregisterAddress(InetSocketAddress addr) throws IllegalAddressException {
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
	public void unregisterID(short id, InetSocketAddress addr) throws IllegalAddressException, IllegalIDException {
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
