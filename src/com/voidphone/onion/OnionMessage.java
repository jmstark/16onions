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

	private final int size;
	private final short id;
	private final InetAddress addr;
	private final byte data[];

	/**
	 * Creates a new Onion message.
	 * 
	 * @param size
	 *            payload size in bytes
	 * @param id
	 *            ID of the logical connection
	 * @param addr
	 *            address of the destination of the logical connection
	 * @param data
	 *            payload
	 */
	public OnionMessage(int size, short id, InetAddress addr, byte[] data) {
		this.size = size;
		this.id = id;
		this.addr = addr;
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
	public static OnionMessage parse(int size, ByteBuffer buf, InetAddress addr) {
		OnionMessage message = null;
		
		buf.flip();
		if (buf.remaining() == ONION_HEADER_SIZE + size) {
			short id = buf.getShort();
			byte data[] = new byte[size];
			buf.get(data);
			message =  new OnionMessage(size, id, addr, data);
		}
		buf.clear();
		return message;
	}

	public void serialize(ByteBuffer buf) {
		buf.clear();
		buf.putShort(id);
		buf.put(data);
		buf.flip();
	}

	/**
	 * Returns the size of the payload.
	 * 
	 * @return the size
	 */
	public int getSize() {
		return size;
	}

	/**
	 * Returns the ID of the logical connection.
	 * 
	 * @return the ID
	 */
	public short getId() {
		return id;
	}

	/**
	 * Returns the address of the destination of the logical connection.
	 * 
	 * @return the address
	 */
	public InetAddress getAddress() {
		return addr;
	}

	/**
	 * Returns the payload.
	 * 
	 * @return the payload.
	 */
	public byte[] getData() {
		return data;
	}
}
