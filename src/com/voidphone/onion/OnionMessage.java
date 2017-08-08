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

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Represents a message sent to/received from another Hop.
 */
public class OnionMessage {
	public static final int ONION_HEADER_SIZE = 2;
	public static final boolean CONTROL_MESSAGE = true;
	public static final boolean DATA_MESSAGE = false;

	/**
	 * the ID of the message
	 */
	public final short id;
	/**
	 * the address of the endpoint
	 */
	public final InetSocketAddress address;
	/**
	 * the size of the payload
	 */
	public final int size;
	/**
	 * the payload of the message
	 */
	public final byte data[];
	/**
	 * the type of the message
	 */
	public final boolean type;

	/**
	 * Creates a new Onion message.
	 * 
	 * @param id
	 *            ID of the logical connection
	 * @param addr
	 *            address of the destination of the logical connection
	 * @param size
	 *            the size of the payload
	 * @param data
	 *            payload
	 */
	public OnionMessage(short id, boolean type, InetSocketAddress addr, int size, byte[] data) {
		this.id = id;
		this.address = addr;
		this.size = size;
		this.data = data;
		this.type = type;
	}

	/**
	 * Creates a new OnionMessage by parsing a buffer.
	 * 
	 * @param buf
	 *            the buffer
	 * @param addr
	 *            the address of the destination of the logical connection
	 * @return the OnionMessage
	 */
	public static OnionMessage parse(ByteBuffer buf, boolean type, InetSocketAddress addr) {
		buf.clear();
		int size = buf.getShort();
		short id = buf.getShort();
		byte data[] = new byte[size];
		buf.get(data);
		buf.clear();
		return new OnionMessage(id, type, addr, size, data);
	}

	/**
	 * Serializes a this onion message into a buffer.
	 * 
	 * @param buf
	 *            the buffer
	 */
	public void serialize(ByteBuffer buf) {
		buf.clear();
		buf.putShort(id);
		buf.put(data);
		buf.clear();
	}

	public String toString() {
		return address + " " + id + " " + Arrays.toString(data);
	}
}
