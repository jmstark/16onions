package com.voidphone.api;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import javax.naming.SizeLimitExceededException;

import com.voidphone.general.General;

import protocol.Connection;
import protocol.DisconnectHandler;
import protocol.MessageHandler;
import protocol.MessageParserException;
import protocol.Protocol;
import protocol.Protocol.MessageType;
import protocol.ProtocolException;

/**
 * This class implements basic methods to simplify the API access. It provides
 * especially a read function
 */
public abstract class ApiSocket {
	protected AsynchronousSocketChannel channel;
	protected Connection connection;
	protected final ByteBuffer readBuffer;
	protected final ByteBuffer writeBuffer;

	/**
	 * Initializes the buffers.
	 * 
	 * @param channel
	 *            the AsynchronousSocketChannel
	 * @throws IOException
	 *             if there is an I/O-error
	 */
	private ApiSocket() throws IOException {
		readBuffer = ByteBuffer.allocate(Protocol.MAX_MESSAGE_SIZE);
		writeBuffer = ByteBuffer.allocate(Protocol.MAX_MESSAGE_SIZE);
	}

	/**
	 * Creates a new API socket and connects it to the specified IP address and port
	 * number.
	 * 
	 * @param addr
	 *            the IP-address and port number
	 * @throws IOException
	 *             if there is an I/O-error
	 */
	public ApiSocket(final InetSocketAddress addr) throws IOException {
		this();
		channel = AsynchronousSocketChannel.open();
		channel.connect(addr, channel, new CompletionHandler<Void, AsynchronousSocketChannel>() {
			@Override
			public void completed(Void none, AsynchronousSocketChannel channel) {
				connection = new Connection(channel, new DisconnectHandler<Void>(null) {
					@Override
					protected void handleDisconnect(Void none) {
						General.fatal("Disconnect from API (" + addr + ")!");
					}
				});
				connection.receive(new MessageHandler<Void>(null) {
					@Override
					public void parseMessage(ByteBuffer buf, MessageType type, Void none)
							throws MessageParserException, ProtocolException {
						receive(buf, type);
					}
				});
				General.info("Connected to API (" + addr + ")");
			}

			@Override
			public void failed(Throwable exception, AsynchronousSocketChannel channel) {
				General.fatal("Cannot connect to API (" + addr + ")!");
			}
		});
	}

	protected abstract void receive(ByteBuffer buffer, MessageType type)
			throws MessageParserException, ProtocolException;
	
	public abstract int register() throws SizeLimitExceededException;
	
	public abstract void unregister(int id) throws IllegalArgumentException;
}
