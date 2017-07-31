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

import java.net.InetAddress;
import java.nio.ByteBuffer;

/**
 * Represents a message sent to/received from another Hop.
 */
public class OnionMessage {
	public static final int ONION_HEADER_SIZE = 2;

	/**
	 * the ID of the message
	 */
	public final short id;
	/**
	 * the address of the endpoint
	 */
	public final InetAddress address;
	/**
	 * the payload of the message
	 */
	public final byte data[];

	/**
	 * Creates a new Onion message.
	 * 
	 * @param id
	 *            ID of the logical connection
	 * @param addr
	 *            address of the destination of the logical connection
	 * @param data
	 *            payload
	 */
	public OnionMessage(int size, short id, InetAddress addr, byte[] data) {
		this.id = id;
		this.address = addr;
		this.data = data;
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
	public static OnionMessage parse(ByteBuffer buf, InetAddress addr) {
		OnionMessage message = null;
		int size = buf.capacity() - ONION_HEADER_SIZE;
		buf.flip();
		if (buf.remaining() == size + ONION_HEADER_SIZE) {
			short id = buf.getShort();
			byte data[] = new byte[size];
			buf.get(data);
			message = new OnionMessage(size, id, addr, data);
		}
		buf.clear();
		return message;
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
		buf.flip();
	}
}
