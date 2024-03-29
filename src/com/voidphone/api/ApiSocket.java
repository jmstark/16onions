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
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import com.voidphone.general.General;
import com.voidphone.general.IllegalIDException;
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
	 * Waits for an API connection on the specified IP address and port number.
	 * 
	 * @param addr
	 *            the IP-address and port number
	 * @param group
	 *            the ChannelGroup to use
	 * @throws IOException
	 *             if there is an I/O-error
	 */
	private void listenForApi(final InetSocketAddress addr, AsynchronousChannelGroup group) throws IOException {
		AsynchronousServerSocketChannel listener = AsynchronousServerSocketChannel.open(group).bind(addr);
		General.info("Waiting for API connection on " + addr + ".....");
		listener.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
			@Override
			public void completed(AsynchronousSocketChannel channel, Void none) {
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
				try {
					General.info("Connected to API (" + ((InetSocketAddress) channel.getRemoteAddress()) + ")");
				} catch (IOException e) {
					General.fatalException(e);
				}
			}

			@Override
			public void failed(Throwable exception, Void none) {
				General.fatal("Cannot connect to API (" + addr + ")!");
			}
		});
	}

	/**
	 * Creates a new API socket and connects it to the specified IP address and port
	 * number.
	 * 
	 * @param addr
	 *            the IP-address and port number
	 * @param group
	 *            the ChannelGroup to use
	 * @throws IOException
	 *             if there is an I/O-error
	 */
	private void connectToApi(final InetSocketAddress addr, AsynchronousChannelGroup group) throws IOException {
		channel = AsynchronousSocketChannel.open(group);
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
	 * Creates a new API socket and connects it to or waits for a connection on the
	 * specified IP address and port number.
	 * 
	 * @param addr
	 *            the IP-address and port number
	 * @param group
	 *            the ChannelGroup to use
	 * @param listen
	 *            should the API wait for a incoming connection?
	 * @throws IOException
	 *             if there is an I/O-error
	 */
	public ApiSocket(final InetSocketAddress addr, AsynchronousChannelGroup group, boolean listen) throws IOException {
		if (listen) {
			listenForApi(addr, group);
		} else {
			connectToApi(addr, group);
		}
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
