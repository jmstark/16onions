package com.voidphone.api;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import protocol.Protocol;

/**
 * This class implements basic methods to simplify the API access. It provides
 * especially a read function
 */
public abstract class ApiSocket {
	protected final SocketChannel channel;
	protected final ByteBuffer readBuffer;
	protected final ByteBuffer writeBuffer;
	private final int timeout;

	/**
	 * Creates a new API socket based on a SocketChannel.
	 * 
	 * @param channel
	 *            the SocketChannel
	 * @param timeout
	 *            timeout for read actions on the API-socket
	 * @throws IOException
	 *             if there is an I/O-error
	 */
	public ApiSocket(SocketChannel channel, int timeout) throws IOException {
		this.channel = channel;
		this.timeout = timeout;
		readBuffer = ByteBuffer.allocate(Protocol.MAX_MESSAGE_SIZE);
		writeBuffer = ByteBuffer.allocate(Protocol.MAX_MESSAGE_SIZE);
	}

	/**
	 * Creates a new API socket and connects it to the specified IP address and port
	 * number.
	 * 
	 * @param addr
	 *            the IP-address and port number
	 * @param timeout
	 *            timeout for read actions on the API-socket
	 * @throws IOException
	 *             if there is an I/O-error
	 */
	public ApiSocket(InetSocketAddress addr, int timeout) throws IOException {
		this(SocketChannel.open(addr), timeout);
	}

	/**
	 * Reads from API connection.
	 * 
	 * @param buffer
	 *            buffer for incoming data
	 * @throws IOException
	 *             if there is an I/O-error
	 */
	protected void read(ByteBuffer buffer) throws IOException {
		Selector selector = Selector.open();
		channel.configureBlocking(false);
		channel.register(selector, SelectionKey.OP_READ);
		if (selector.select(timeout) == 0) {
			throw new SocketTimeoutException("API request timed out!");
		}
		SelectionKey key = selector.selectedKeys().iterator().next();
		key.cancel();
		selector.selectNow();
		key.channel().configureBlocking(true);
		channel.read(buffer);
	}

	/**
	 * Writes to API connection.
	 * 
	 * @param buffer
	 *            buffer for incoming data
	 * @throws IOException
	 *             if there is an I/O-error
	 */
	protected void write(ByteBuffer buffer) throws IOException {
		Selector selector = Selector.open();
		channel.configureBlocking(false);
		channel.register(selector, SelectionKey.OP_WRITE);
		if (selector.select(timeout) == 0) {
			throw new SocketTimeoutException("API request timed out!");
		}
		SelectionKey key = selector.selectedKeys().iterator().next();
		key.cancel();
		selector.selectNow();
		key.channel().configureBlocking(true);
		channel.write(buffer);
	}
}
