package com.voidphone.onion;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import com.voidphone.general.SizeLimitExceededException;

public class Multiplexer {
	// TODO: identify connections by (source IP,ID)
	private final HashMap<Short, LinkedBlockingQueue<OnionMessage>> map;
	private final SecureRandom random;

	public Multiplexer() {
		random = new SecureRandom();
		map = new HashMap<Short, LinkedBlockingQueue<OnionMessage>>();
	}

	public LinkedBlockingQueue<OnionMessage> getQueue(short id) {
		if (!map.containsKey(id)) {
			throw new IllegalArgumentException("Illegal ID!");
		}
		return map.get(id);
	}

	public short register() throws SizeLimitExceededException {
		short id;

		if (map.size() >= Short.MAX_VALUE) {
			throw new SizeLimitExceededException("Too many connections registered!");
		}
		do {
			id = (short) random.nextInt();
		} while (map.containsKey(id));
		map.put(id, new LinkedBlockingQueue<OnionMessage>());
		return id;
	}

	public void register(short id) throws SizeLimitExceededException {
		if (map.size() >= Short.MAX_VALUE) {
			throw new SizeLimitExceededException("Too many connections registered!");
		}
		if (map.containsKey(id)) {
			throw new IllegalArgumentException("Illegal ID!");
		}
		map.put(id, new LinkedBlockingQueue<OnionMessage>());
	}

	public void unregister(short id) throws IllegalArgumentException {
		if (!map.containsKey(id)) {
			throw new IllegalArgumentException("Illegal ID!");
		}
		map.remove(id);
	}
}
