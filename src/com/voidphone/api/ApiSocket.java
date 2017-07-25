package com.voidphone.api;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import protocol.Protocol;

public abstract class ApiSocket {
	protected final SocketChannel channel;
	protected final ByteBuffer readBuffer;
	protected final ByteBuffer writeBuffer;

	public ApiSocket(SocketChannel channel) throws IOException {
		this.channel = channel;
		readBuffer = ByteBuffer.allocate(Protocol.MAX_MESSAGE_SIZE);
		writeBuffer = ByteBuffer.allocate(Protocol.MAX_MESSAGE_SIZE);
	}

	public ApiSocket(InetSocketAddress addr) throws IOException {
		this(SocketChannel.open(addr));
	}

	protected void read(ByteBuffer buffer) throws IOException {
		Selector selector = Selector.open();
		channel.configureBlocking(false);
		channel.register(selector, SelectionKey.OP_READ);
		if (selector.select(1000) == 0) {
			throw new SocketTimeoutException("API request timed out!");
		}
		SelectionKey key = selector.selectedKeys().iterator().next();
		key.cancel();
		selector.selectNow();
		key.channel().configureBlocking(true);
		channel.read(buffer);
	}

	protected void write(ByteBuffer buffer) throws IOException {
		channel.write(buffer);
	}

	public void close() throws IOException {
		channel.close();
	}
}
