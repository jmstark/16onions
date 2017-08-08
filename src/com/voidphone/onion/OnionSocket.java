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
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

import com.voidphone.general.General;
import com.voidphone.general.IllegalAddressException;
import com.voidphone.general.IllegalIDException;
import com.voidphone.general.SizeLimitExceededException;

public class OnionSocket {
	// TODO: Move backup tunnels into this class, update currentIncomingTunnel
	private InetSocketAddress address;
	private AsynchronousSocketChannel channel;
	private ByteBuffer readBuffer;
	private ByteBuffer writeBuffer;
	private Multiplexer multiplexer;
	private ReentrantLock writeLock;

	/**
	 * Creates an OnionSocket.
	 * 
	 * @param m
	 *            the multiplexer
	 * @param ch
	 *            the channel
	 * @throws IOException
	 *             if there is an I/O-error
	 * @throws IllegalAddressException
	 *             if the address is already registered
	 */
	public OnionSocket(Multiplexer m, AsynchronousSocketChannel ch) throws IOException, IllegalAddressException {
		init(m, ch);
		m.registerAddress(address, this);
		channel.read(readBuffer, null, new ReadCompletionHandler());
	}

	private void init(Multiplexer m, AsynchronousSocketChannel ch) throws IOException, IllegalAddressException {
		readBuffer = ByteBuffer.allocate(Main.getConfig().onionSize + OnionMessage.ONION_HEADER_SIZE);
		writeBuffer = ByteBuffer.allocate(Main.getConfig().onionSize + OnionMessage.ONION_HEADER_SIZE);
		this.multiplexer = m;
		this.writeLock = new ReentrantLock();
		this.channel = ch;
		this.address = (InetSocketAddress) channel.getRemoteAddress();
	}

	public OnionSocket(Multiplexer m, InetSocketAddress addr)
			throws IOException, IllegalAddressException, TimeoutException, InterruptedException {
		channel = AsynchronousSocketChannel.open();
		Future<Void> future = channel.connect(addr);
		try {
			future.get(Main.getConfig().onionTimeout, TimeUnit.MILLISECONDS);
		} catch (ExecutionException e) {
			throw new IOException(e.getCause().getMessage());
		}
		init(m, channel);
		channel.read(readBuffer, null, new ReadCompletionHandler());
	}

	/**
	 * This function is called whenever a previously unknown address initiates a
	 * connection to this peer. The function tries to establish an onion connection
	 * and then processes and/or forwards all the traffic. It does only return after
	 * the tunnel has been destroyed or broken.
	 * 
	 * @param m
	 * @param id
	 * @param addr
	 */
	private void newConnection(Multiplexer m, short id, InetSocketAddress addr) {
		try {
			OnionListenerSocket incomingSocket = new OnionListenerSocket(addr, m, id);
			incomingSocket.authenticate();
			boolean tunnelDestroyed = false;
			while (!tunnelDestroyed) {
				tunnelDestroyed = incomingSocket.getAndProcessNextMessage();
			}
		} catch (Exception e) {
			// The tunnel has broken
			e.printStackTrace();
		}
	}

	public void send(OnionMessage message) throws InterruptedException, IOException {
		if (!channel.isOpen()) {
			throw new IOException("Socket is closed!");
		}
		writeLock.lock();
		message.serialize(writeBuffer);
		while (writeBuffer.hasRemaining()) {
			boolean error = true;
			try {
				if (channel.write(writeBuffer).get(Main.getConfig().onionTimeout, TimeUnit.MILLISECONDS) > 0) {
					error = false;
				}
			} catch (TimeoutException e) {
			} catch (ExecutionException e) {
				General.error(e.getMessage());
			}
			if (error) {
				close();
				throw new IOException("write failed!");
			}
		}
		writeLock.unlock();
	}

	public void close() {
		General.info("Closing connection to " + address);
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
			if (!channel.isOpen()) {
				return;
			}
			if (length <= 0) {
				close();
				return;
			}
			OnionMessage message = OnionMessage.parse(readBuffer, OnionMessage.CONTROL_MESSAGE, address);
			try {
				multiplexer.getReadQueue(message.id, message.address).offer(message);
			} catch (IllegalAddressException e) {
				General.warning("Got packet with wrong address (" + message.address + ")!");
			} catch (IllegalIDException e) {
				try {
					General.debug("Got packet with unknown ID - maybe a new connection.....");
					multiplexer.registerID(message.id, address);
					multiplexer.getReadQueue(message.id, message.address).offer(message);
					channel.read(readBuffer, null, this);
					newConnection(multiplexer, message.id, address);
					return;
				} catch (SizeLimitExceededException f) {
					General.warning(f.getMessage());
				} catch (IllegalIDException f) {
					General.warning("Got packet with illegal ID!");
				} catch (IllegalAddressException f) {
					General.error("Address is not registered, but should be!");
					close();
				}
			}
			channel.read(readBuffer, null, this);
		}

		@Override
		public void failed(Throwable ex, Void none) {
			close();
		}
	}
}
