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
package com.voidphone.onion;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import com.voidphone.general.General;
import com.voidphone.general.IllegalAddressException;
import com.voidphone.general.IllegalIDException;
import com.voidphone.general.SizeLimitExceededException;

public class OnionSocket {
	private final InetAddress address;
	private final AsynchronousSocketChannel channel;
	private final ByteBuffer readBuffer;
	private final ByteBuffer writeBuffer;
	private final Multiplexer multiplexer;

	/**
	 * Creates a OnionSocket.
	 * 
	 * @param m
	 *            the multiplexer
	 * @param cch
	 *            the channel
	 * @throws IOException
	 *             if there is an I/O-error
	 */
	public OnionSocket(Multiplexer m, AsynchronousSocketChannel cch) throws IOException {
		readBuffer = ByteBuffer.allocate(Main.getConfig().onionSize + OnionMessage.ONION_HEADER_SIZE);
		writeBuffer = ByteBuffer.allocate(Main.getConfig().onionSize + OnionMessage.ONION_HEADER_SIZE);
		this.channel = cch;
		this.address = ((InetSocketAddress) channel.getRemoteAddress()).getAddress();
		this.multiplexer = m;
		channel.read(readBuffer, null, new ReadCompletionHandler());
	}

	private void newConnection(Multiplexer m, short id, InetAddress addr) {
		// TODO: handle new connection
	}

	public void send(OnionMessage message) {
		message.serialize(writeBuffer);
		channel.write(writeBuffer, message, new WriteCompletionHandler());
	}

	public void close() {
		try {
			multiplexer.unregister(address);
		} catch (IllegalAddressException e) {
			General.warning("Address is not registered, but it should be!");
		}
		try {
			if (channel.isOpen()) {
				channel.close();
			}
		} catch (IOException e) {
			General.error("TCP channel close failed!");
		}
	}

	private class ReadCompletionHandler implements CompletionHandler<Integer, Void> {
		@Override
		public void completed(Integer length, Void none) {
			if (length <= 0) {
				close();
				return;
			}
			OnionMessage message = OnionMessage.parse(Main.getConfig().onionSize, readBuffer, address);
			channel.read(readBuffer, null, this);
			try {
				multiplexer.getReadQueue(message.getId(), message.getAddress()).offer(message);
			} catch (IllegalAddressException e) {
				General.warning("Got packet with wrong address!");
			} catch (IllegalIDException e) {
				try {
					multiplexer.register(message.getId(), address);
					multiplexer.getReadQueue(message.getId(), message.getAddress()).offer(message);
					newConnection(multiplexer, message.getId(), address);
				} catch (SizeLimitExceededException f) {
					General.warning(f.getMessage());
				} catch (IllegalIDException f) {
					General.warning("Got packet with illegal ID!");
				} catch (IllegalAddressException f) {
					General.error("Address is not registered, but should be!");
					close();
				}
			}
		}

		@Override
		public void failed(Throwable ex, Void none) {
			close();
		}
	}

	private class WriteCompletionHandler implements CompletionHandler<Integer, OnionMessage> {
		@Override
		public void completed(Integer result, OnionMessage message) {
			if (result <= 0) {
				close();
				return;
			}
			if (writeBuffer.hasRemaining()) {
				channel.write(writeBuffer, null, this);
				return;
			}
			try {
				multiplexer.getWriteQueue(message.getId(), message.getAddress()).offer(null);
			} catch (IllegalAddressException | IllegalIDException e) {
				General.error("Address or ID not registered, but should be!");
				close();
			}
		}

		@Override
		public void failed(Throwable exception, OnionMessage message) {
			close();
		}
	}
}
