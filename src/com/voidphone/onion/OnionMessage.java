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
