package com.voidphone.api;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import com.voidphone.general.General;
import com.voidphone.general.SizeLimitExceededException;

import protocol.Connection;
import protocol.DisconnectHandler;
import protocol.MessageHandler;
import protocol.MessageParserException;
import protocol.Protocol.MessageType;
import protocol.ProtocolException;

/**
 * This class implements basic methods to simplify the API access.
 */
public abstract class ApiSocket {
	protected AsynchronousSocketChannel channel;
	protected Connection connection;

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

	/**
	 * Listens for a new API connection.
	 * 
	 * @param port
	 *            the port to listen on
	 * @throws IOException
	 *             if there is an I/O-error
	 */
	public ApiSocket(int port) throws IOException {
		AsynchronousServerSocketChannel listener = AsynchronousServerSocketChannel.open()
				.bind(new InetSocketAddress(port));
		listener.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
			@Override
			public void completed(AsynchronousSocketChannel channel, Void none) {
				connection = new Connection(channel, new DisconnectHandler<Void>(null) {
					@Override
					protected void handleDisconnect(Void none) {
						General.fatal("Disconnect from API!");
					}
				});
				connection.receive(new MessageHandler<Void>(null) {
					@Override
					public void parseMessage(ByteBuffer buf, MessageType type, Void none)
							throws MessageParserException, ProtocolException {
						receive(buf, type);
					}
				});
				General.info("Connected to API");
			}

			@Override
			public void failed(Throwable exception, Void none) {
				General.fatal("Cannot connect to API!");
			}
		});
	}

	/**
	 * Called when a new packet arrives.
	 * 
	 * @param buffer
	 *            contains the packet
	 * @param type
	 *            the type of the packet
	 * @throws MessageParserException
	 *             if there is an error while parsing the message
	 * @throws ProtocolException
	 *             if the message does not match the protocol
	 */
	protected abstract void receive(ByteBuffer buffer, MessageType type)
			throws MessageParserException, ProtocolException;

	/**
	 * Registers a new logical connection to the API.
	 * 
	 * @return ID of the new connection
	 * @throws SizeLimitExceededException
	 *             if too many connections are registered
	 */
	public abstract int register() throws SizeLimitExceededException;

	/**
	 * Unregisters a logical connection.
	 * 
	 * @param id
	 *            ID of the connection
	 * @throws IllegalArgumentException
	 *             if the ID was not registered
	 */
	public abstract void unregister(int id) throws IllegalArgumentException;
}
