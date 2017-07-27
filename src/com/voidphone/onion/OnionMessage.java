package com.voidphone.onion;

import java.nio.ByteBuffer;

public class OnionMessage {
	private final int size;
	private final short id;
	private final byte data[];

	public OnionMessage(int size, short id, byte[] data) {
		this.size = size;
		this.id = id;
		this.data = data;
	}
	
	public static OnionMessage parse(ByteBuffer buf) {
		buf.flip();
		if (buf.remaining() >= 4) {
			int size = ((int) buf.getShort() & 0xffff);
			short id = buf.getShort();
			if (buf.remaining() >= size) {
				byte data[] = new byte[size];
				buf.get(data);
				buf.compact();
				return new OnionMessage(size, id, data);
			}
		}
		buf.rewind();
		buf.compact();
		return null;
	}

	public int getSize() {
		return size;
	}

	public short getId() {
		return id;
	}

	public byte[] getData() {
		return data;
	}
}
