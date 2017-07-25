package com.voidphone.api;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import protocol.Protocol;

public abstract class ApiSocket {
	protected final SocketChannel channel;
	protected final ByteBuffer readBuffer;
	protected final ByteBuffer writeBuffer;

	public ApiSocket(SocketChannel channel) throws IOException {
		channel.socket().setSoTimeout(1000);
		this.channel = channel;
		readBuffer = ByteBuffer.allocate(Protocol.MAX_MESSAGE_SIZE);
		writeBuffer = ByteBuffer.allocate(Protocol.MAX_MESSAGE_SIZE);
	}

	public ApiSocket(InetSocketAddress addr) throws IOException {
		this(SocketChannel.open(addr));
	}

	public final SocketChannel getChannel() {
		return channel;
	}

	public void close() throws IOException {
		channel.close();
	}
}
