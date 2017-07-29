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
package com.voidphone.api;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import com.voidphone.general.General;
import com.voidphone.general.IllegalIDException;
import com.voidphone.general.SizeLimitExceededException;
import com.voidphone.onion.Main;

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
		channel = AsynchronousSocketChannel.open(Main.getConfig().group);
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
		AsynchronousServerSocketChannel listener = AsynchronousServerSocketChannel.open(Main.getConfig().group)
				.bind(new InetSocketAddress(port));
		General.info("Waiting for API connection on " + port + ".....");
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
				General.info("API connection successful");
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
	 * @throws IllegalIDException
	 *             if the ID is not registered
	 */
	public abstract void unregister(int id) throws IllegalIDException;
}
