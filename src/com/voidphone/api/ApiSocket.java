package com.voidphone.api;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import protocol.Protocol;

/**
 * partly copied from Connection in testing
 */
public abstract class ApiSocket {
	protected final SocketChannel channel;
	protected final ByteBuffer readBuffer;
	protected final ByteBuffer writeBuffer;

	public ApiSocket(InetSocketAddress addr) throws IOException {
		channel = SocketChannel.open();
		// TODO: set timeout
		channel.connect(addr);
		readBuffer = ByteBuffer.allocate(Protocol.MAX_MESSAGE_SIZE);
		writeBuffer = ByteBuffer.allocate(Protocol.MAX_MESSAGE_SIZE);
	}

	public final SocketChannel getChannel() {
		return channel;
	}

	public void close() throws IOException {
		channel.close();
	}
}
