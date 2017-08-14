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
import java.nio.channels.DatagramChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

import com.voidphone.general.General;
import com.voidphone.general.IllegalAddressException;
import com.voidphone.general.IllegalIDException;
import com.voidphone.general.SizeLimitExceededException;
import com.voidphone.general.TunnelCrashException;

public class OnionSocket {
	// TODO: Move backup tunnels into this class, update currentIncomingTunnel
	private final InetSocketAddress address;
	private AsynchronousSocketChannel controlChannel;
	private DatagramChannel dataChannel;
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
	 * @throws TimeoutException
	 * @throws InterruptedException
	 */
	public OnionSocket(Multiplexer m, AsynchronousSocketChannel cch, DatagramChannel dch) throws IOException {
		dataChannel = dch;
		init(m, cch);
		ByteBuffer buf = ByteBuffer.allocate(2);
		Future<Integer> future = controlChannel.read(buf);
		try {
			do {
				if (future.get(Main.getConfig().onionTimeout, TimeUnit.MILLISECONDS) <= 0) {
					close();
					throw new IOException("Cannot read the remote port number!");
				}
			} while (buf.hasRemaining());
		} catch (ExecutionException | InterruptedException | TimeoutException e) {
			close();
			throw new IOException("Cannot read the remote port number!");
		}
		buf.flip();
		int port = Short.toUnsignedInt(buf.getShort());
		this.address = new InetSocketAddress(((InetSocketAddress) controlChannel.getRemoteAddress()).getAddress(),
				port);
		General.debug("Got connection from " + address + " to " + (InetSocketAddress) controlChannel.getLocalAddress());
		try {
			m.registerAddress(address, this);
		} catch (IllegalAddressException e) {
			close();
			throw new IOException(e.getMessage());
		}
		controlChannel.read(readBuffer, null, new ReadCompletionHandler());
	}

	public OnionSocket(Multiplexer m, InetSocketAddress caddr, DatagramChannel dch) throws IOException {
		dataChannel = dch;
		controlChannel = AsynchronousSocketChannel.open();
		Future<Void> future = controlChannel.connect(caddr);
		try {
			future.get(Main.getConfig().onionTimeout, TimeUnit.MILLISECONDS);
		} catch (ExecutionException | TimeoutException | InterruptedException e) {
			throw new IOException("Cannot connect to " + caddr + "!");
		}
		init(m, controlChannel);
		this.address = caddr;
		ByteBuffer buf = ByteBuffer.allocate(2);
		buf.putShort((short) dch.socket().getLocalPort());
		buf.flip();
		while (buf.hasRemaining()) {
			try {
				if (controlChannel.write(buf).get(Main.getConfig().onionTimeout, TimeUnit.MILLISECONDS) <= 0) {
					close();
					throw new IOException("Cannot send the local port number!");
				}
			} catch (ExecutionException | TimeoutException | InterruptedException e) {
				close();
				throw new IOException("Cannot send the local port number!");
			}
		}
		controlChannel.read(readBuffer, null, new ReadCompletionHandler());
	}

	private void init(Multiplexer m, AsynchronousSocketChannel ch) throws IOException {
		readBuffer = ByteBuffer.allocate(Main.getConfig().onionSize + OnionMessage.ONION_HEADER_SIZE);
		writeBuffer = ByteBuffer.allocate(Main.getConfig().onionSize + OnionMessage.ONION_HEADER_SIZE);
		this.multiplexer = m;
		this.writeLock = new ReentrantLock();
		this.controlChannel = ch;
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
		OnionBaseSocket tunnel;
		OnionConnectingSocket ocs = Main.getOas().getAndRemoveDetachedTunnelById(id);
		if (ocs != null) {
			try {
				ocs.constructTunnel(id, addr);
			} catch (TunnelCrashException e) {
				General.warning("Incoming tunnel crashed!");
				return;
			}
			tunnel = ocs;
		} else {
			OnionListenerSocket incomingSocket;
			try {
				incomingSocket = new OnionListenerSocket(addr, m, id);
			} catch (SizeLimitExceededException e) {
				General.error(e.getMessage());
				return;
			}
			try {
				incomingSocket.authenticate();
			} catch (TunnelCrashException e) {
				General.error("Tunnel crashed!");
				return;
			}
			tunnel = incomingSocket;
		}
		// the tunnel handler
		boolean tunnelDestroyed = false;
		while (!tunnelDestroyed) {
			try {
				tunnelDestroyed = tunnel.getAndProcessNextMessage();
			} catch (InterruptedException e) {
				General.fatalException(e);
			} catch (TunnelCrashException e) {
				General.error("Tunnel crashed!");
				return;
			}
		}
	}

	public void send(OnionMessage message) throws InterruptedException, IOException, SizeLimitExceededException {
		if (!controlChannel.isOpen()) {
			throw new IOException("Socket is closed!");
		}
		writeLock.lock();
		message.serialize(writeBuffer);
		if (message.type == OnionMessage.CONTROL_MESSAGE) {
			while (writeBuffer.hasRemaining()) {
				boolean error = true;
				try {
					if (controlChannel.write(writeBuffer).get(Main.getConfig().onionTimeout,
							TimeUnit.MILLISECONDS) > 0) {
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
			General.info("Sent control packet!");
		} else {
			dataChannel.send(writeBuffer, address);
			General.info("Sent data packet!");
		}
		writeLock.unlock();
	}

	public void close() {
		General.info("Closing connection to " + address);
		try {
			if (controlChannel.isOpen()) {
				controlChannel.close();
			}
		} catch (IOException e) {
			General.error("TCP channel close failed!");
		}
	}

	private class ReadCompletionHandler implements CompletionHandler<Integer, Void> {
		@Override
		public void completed(Integer length, Void none) {
			if (!controlChannel.isOpen()) {
				return;
			}
			if (length <= 0) {
				close();
				return;
			}
			if (readBuffer.hasRemaining()) {
				controlChannel.read(readBuffer, null, this);
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
					controlChannel.read(readBuffer, null, this);
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
			controlChannel.read(readBuffer, null, this);
		}

		@Override
		public void failed(Throwable ex, Void none) {
			close();
		}
	}
}
