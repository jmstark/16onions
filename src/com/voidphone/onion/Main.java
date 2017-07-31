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
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.InvalidKeyException;
import java.util.Iterator;

import org.ini4j.InvalidFileFormatException;

import lombok.Getter;

import com.voidphone.api.Config;
import com.voidphone.api.OnionApiSocket;
import com.voidphone.api.OnionAuthApiSocket;
import com.voidphone.api.RpsApiSocket;
import com.voidphone.general.General;
import com.voidphone.general.IllegalAddressException;
import com.voidphone.general.IllegalIDException;

import protocol.MessageParserException;
import protocol.ProtocolException;

public class Main {
	private static @Getter Selector selector;
	private static @Getter Config config;
	private static @Getter OnionApiSocket oas;

	private static void run() throws IOException {
		final DatagramChannel dataChannel;
		final OnionApiSocket onionApiSocket;
		final AsynchronousServerSocketChannel onionServerSocket;
		final Multiplexer multiplexer;
		final ByteBuffer readBuffer;

		readBuffer = ByteBuffer.allocate(config.onionSize + OnionMessage.ONION_HEADER_SIZE);
		dataChannel = DatagramChannel.open().bind(new InetSocketAddress(1234));
		multiplexer = new Multiplexer(dataChannel, config.onionSize);
		onionApiSocket = new OnionApiSocket(config.onionAPIPort);
		General.info("Waiting for Onion connections on " + config.onionPort + ".....");
		onionServerSocket = AsynchronousServerSocketChannel.open(config.group)
				.bind(new InetSocketAddress(config.onionPort));
		onionServerSocket.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
			@Override
			public void completed(AsynchronousSocketChannel channel, Void none) {
				onionServerSocket.accept(null, this);
				try {
					new OnionSocket(multiplexer, channel);
				} catch (IOException e) {
					General.error("I/O error!");
				} catch (IllegalAddressException e) {
					General.warning("Got multiple TCP channel from one Hop!");
				}
				General.info("Onion connection successful");
			}

			@Override
			public void failed(Throwable exception, Void none) {
				General.fatal("Accept failed!");
			}
		});
		while (true) {
			InetAddress addr = ((InetSocketAddress) dataChannel.receive(readBuffer)).getAddress();
			OnionMessage message = OnionMessage.parse(readBuffer, addr);
			try {
				multiplexer.getReadQueue(message.id, message.address).offer(message);
			} catch (IllegalAddressException | IllegalIDException e) {
				General.warning("Got packet with wrong address or ID!");
			}
			readBuffer.clear();
		}
	}

	/**
	 * Runs the Onion module.
	 * 
	 * @throws Exception
	 */
	/*
	 * private static void run() throws Exception { try { // socket for all incoming
	 * UDP packets, // for OnionConnectingSocket as well as OnionListenerSocket
	 * DatagramChannel udpChannel = DatagramChannel.open() .bind(new
	 * InetSocketAddress("127.0.0.1", config.getOnionPort()));
	 * 
	 * selector = Selector.open(); General.info("Waiting for API connection on " +
	 * config.getOnionApiPort() + "....."); SocketChannel onionApiSocket =
	 * ServerSocketChannel.open() .bind(new InetSocketAddress("127.0.0.1",
	 * config.getOnionApiPort())).accept();
	 * General.debug("API connection successful");
	 * General.info("Waiting for Onion connections on " + config.getOnionPort() +
	 * "....."); ServerSocketChannel onionServerSocket = ServerSocketChannel.open()
	 * .bind(new InetSocketAddress("127.0.0.1", config.getOnionPort()));
	 * 
	 * // for API requests oas = new OnionApiSocket(onionApiSocket, config);
	 * onionApiSocket.configureBlocking(false); onionApiSocket.register(selector,
	 * SelectionKey.OP_READ, oas);
	 * 
	 * // for incoming Onion connections onionServerSocket.configureBlocking(false);
	 * onionServerSocket.register(selector, SelectionKey.OP_ACCEPT);
	 * 
	 * // wait for any socket getting ready while (selector.select() != 0) {
	 * Iterator<SelectionKey> iterator = selector.selectedKeys().iterator(); while
	 * (iterator.hasNext()) { SelectionKey key = iterator.next(); if
	 * (key.isAcceptable()) { // OnionServerSocket got a connection request
	 * General.debug("Onion connection requested....."); SocketChannel onionSocket =
	 * onionServerSocket.accept(); General.debug("Onion connection successful"); //
	 * create a new OnionListenerSocket ... OnionListenerSocket ols = new
	 * OnionListenerSocket(onionSocket.socket(), config);
	 * General.debug("Got connection from " + onionSocket.getRemoteAddress());
	 * onionSocket.configureBlocking(false); // ... and add it to the selector
	 * onionSocket.register(selector, SelectionKey.OP_READ, ols); } else if
	 * (key.isReadable()) { // an OnionListenerSocket or the OnionAPISocket received
	 * // data General.debug("Received packet"); // enable blocking key.cancel();
	 * selector.selectNow(); key.channel().configureBlocking(true); // handle the
	 * received data boolean again = ((Attachable) key.attachment()).handle(); if
	 * (!again) { // the connection is still alive
	 * key.channel().configureBlocking(false); key.channel().register(selector,
	 * SelectionKey.OP_READ, key.attachment()); } } else {
	 * General.fatal("Selector returns unknown key!"); } }
	 * selector.selectedKeys().clear(); } } catch (IOException |
	 * MessageParserException | ProtocolException e) { General.fatalException(e); }
	 * }
	 */

	private static void parseArgs(String args[]) {
		if (args.length < 2 || !"-c".equals(args[0])) {
			System.out.println("Usage: java Main -c <path_to_config_file>");
			System.exit(1);
		}
		try {
			config = new Config(args[1]);
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidFileFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 *            Command line arguments
	 * 
	 * @throws IOException
	 */
	public static void main(String[] args) throws Exception {
		General.initDebugging();

		parseArgs(args);

		run();
	}

	public static interface Attachable {
		public boolean handle() throws Exception, IOException, MessageParserException, ProtocolException;
	}
}
