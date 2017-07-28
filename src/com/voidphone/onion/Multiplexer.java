package com.voidphone.onion;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.voidphone.general.IllegalAddressException;
import com.voidphone.general.IllegalIDException;
import com.voidphone.general.SizeLimitExceededException;
import com.voidphone.general.Triple;
import com.voidphone.general.Double;
import com.voidphone.general.General;

public class Multiplexer {
	private final ReentrantReadWriteLock firstLock;
	private final HashMap<InetAddress, Triple<ReentrantReadWriteLock, OnionSocket, HashMap<Short, Double<LinkedBlockingQueue<OnionMessage>, LinkedBlockingQueue<Void>>>>> first;
	private final SecureRandom random;
	private final ByteBuffer writeBuffer;
	private final DatagramChannel channel;

	public Multiplexer(DatagramChannel channel, int size) {
		random = new SecureRandom();
		first = new HashMap<InetAddress, Triple<ReentrantReadWriteLock, OnionSocket, HashMap<Short, Double<LinkedBlockingQueue<OnionMessage>, LinkedBlockingQueue<Void>>>>>();
		firstLock = new ReentrantReadWriteLock(true);
		writeBuffer = ByteBuffer.allocate(size + OnionMessage.ONION_HEADER_SIZE);
		this.channel = channel;
	}

	public OnionMessage read(short id, InetAddress addr) throws IllegalAddressException, IllegalIDException {
		return getReadQueue(id, addr).poll();
	}

	public void writeControl(OnionMessage message) throws IllegalAddressException, IllegalIDException {
		LinkedBlockingQueue<Void> queue = getWriteQueue(message.getId(), message.getAddress());
		getOnionSocket(message.getAddress()).send(message);
		queue.poll();
	}

	public void writeData(OnionMessage message) throws IllegalAddressException, IllegalIDException {
		LinkedBlockingQueue<Void> queue = getWriteQueue(message.getId(), message.getAddress());
		send(message);
		queue.poll();
	}

	private void send(OnionMessage message) {
		message.serialize(writeBuffer);
		try {
			channel.write(writeBuffer);
		} catch (IOException e) {
			General.fatalException(e);
		}
	}

	private OnionSocket getOnionSocket(InetAddress addr) throws IllegalAddressException {
		return getFirst(addr).y;
	}

	private Triple<ReentrantReadWriteLock, OnionSocket, HashMap<Short, Double<LinkedBlockingQueue<OnionMessage>, LinkedBlockingQueue<Void>>>> getFirst(
			InetAddress addr) throws IllegalAddressException {
		firstLock.readLock().lock();
		if (!first.containsKey(addr)) {
			throw new IllegalAddressException();
		}
		Triple<ReentrantReadWriteLock, OnionSocket, HashMap<Short, Double<LinkedBlockingQueue<OnionMessage>, LinkedBlockingQueue<Void>>>> ret = first
				.get(addr);
		firstLock.readLock().unlock();
		return ret;
	}

	private Double<LinkedBlockingQueue<OnionMessage>, LinkedBlockingQueue<Void>> getSecond(
			Triple<ReentrantReadWriteLock, OnionSocket, HashMap<Short, Double<LinkedBlockingQueue<OnionMessage>, LinkedBlockingQueue<Void>>>> triple,
			Short id) throws IllegalIDException {
		triple.x.readLock().lock();
		if (!triple.z.containsKey(id)) {
			throw new IllegalIDException();
		}
		Double<LinkedBlockingQueue<OnionMessage>, LinkedBlockingQueue<Void>> ret = triple.z.get(id);
		triple.x.readLock().unlock();
		return ret;
	}

	public LinkedBlockingQueue<OnionMessage> getReadQueue(short id, InetAddress addr)
			throws IllegalAddressException, IllegalIDException {
		return getSecond(getFirst(addr), id).x;
	}

	public LinkedBlockingQueue<Void> getWriteQueue(short id, InetAddress addr)
			throws IllegalAddressException, IllegalIDException {
		return getSecond(getFirst(addr), id).y;
	}

	public void register(InetAddress addr, OnionSocket sock) throws IllegalAddressException {
		firstLock.writeLock().lock();
		if (first.containsKey(addr)) {
			throw new IllegalAddressException();
		}
		first.put(addr,
				new Triple<ReentrantReadWriteLock, OnionSocket, HashMap<Short, Double<LinkedBlockingQueue<OnionMessage>, LinkedBlockingQueue<Void>>>>(
						new ReentrantReadWriteLock(), sock,
						new HashMap<Short, Double<LinkedBlockingQueue<OnionMessage>, LinkedBlockingQueue<Void>>>()));
		firstLock.writeLock().unlock();
	}

	public short register(InetAddress addr) throws SizeLimitExceededException, IllegalAddressException {
		short id;
		HashMap<Short, Double<LinkedBlockingQueue<OnionMessage>, LinkedBlockingQueue<Void>>> second;
		Triple<ReentrantReadWriteLock, OnionSocket, HashMap<Short, Double<LinkedBlockingQueue<OnionMessage>, LinkedBlockingQueue<Void>>>> triple;
		triple = getFirst(addr);
		triple.x.writeLock().lock();
		second = triple.z;
		if (second.size() >= Short.MAX_VALUE) {
			throw new SizeLimitExceededException("Too many connections registered!");
		}
		do {
			id = (short) random.nextInt();
		} while (second.containsKey(id));
		second.put(id, new Double<LinkedBlockingQueue<OnionMessage>, LinkedBlockingQueue<Void>>(
				new LinkedBlockingQueue<OnionMessage>(), new LinkedBlockingQueue<Void>(1)));
		triple.x.writeLock().unlock();
		return id;
	}

	public void register(short id, InetAddress addr)
			throws SizeLimitExceededException, IllegalIDException, IllegalAddressException {
		HashMap<Short, Double<LinkedBlockingQueue<OnionMessage>, LinkedBlockingQueue<Void>>> second;
		Triple<ReentrantReadWriteLock, OnionSocket, HashMap<Short, Double<LinkedBlockingQueue<OnionMessage>, LinkedBlockingQueue<Void>>>> triple;
		triple = getFirst(addr);
		triple.x.writeLock().lock();
		second = triple.z;
		if (second.size() >= Short.MAX_VALUE) {
			throw new SizeLimitExceededException("Too many connections registered!");
		}
		if (second.containsKey(id)) {
			throw new IllegalIDException();
		}
		second.put(id, new Double<LinkedBlockingQueue<OnionMessage>, LinkedBlockingQueue<Void>>(
				new LinkedBlockingQueue<OnionMessage>(), new LinkedBlockingQueue<Void>(1)));
		triple.x.writeLock().unlock();
	}

	public void unregister(InetAddress addr) throws IllegalAddressException {
		firstLock.writeLock().lock();
		if (!first.containsKey(addr)) {
			throw new IllegalAddressException();
		}
		first.remove(addr);
		firstLock.writeLock().unlock();
	}

	public void unregister(short id, InetAddress addr) throws IllegalAddressException, IllegalIDException {
		HashMap<Short, Double<LinkedBlockingQueue<OnionMessage>, LinkedBlockingQueue<Void>>> second;
		Triple<ReentrantReadWriteLock, OnionSocket, HashMap<Short, Double<LinkedBlockingQueue<OnionMessage>, LinkedBlockingQueue<Void>>>> triple;
		triple = getFirst(addr);
		triple.x.writeLock().lock();
		second = triple.z;
		if (!second.containsKey(id)) {
			throw new IllegalIDException();
		}
		second.remove(id);
		if (second.isEmpty()) {
			triple.x.writeLock().unlock();
			triple.y.close();
		} else {
			triple.x.writeLock().unlock();
		}
	}
}
