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
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Selector;
import java.security.InvalidKeyException;

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
	// TODO: How and where do we initialise oaas and ras and oas?
	private static @Getter OnionAuthApiSocket oaas;
	private static @Getter RpsApiSocket ras;
	private static @Getter Multiplexer multiplexer;

	/**
	 * Runs the Onion module.
	 * 
	 * @throws Exception
	 */
	private static void run() throws IOException {
		final DatagramChannel dataChannel;
		final AsynchronousServerSocketChannel onionServerSocket;
		final ByteBuffer readBuffer;

		oaas = new OnionAuthApiSocket(new InetSocketAddress(config.onionAuthAPIAddress, config.onionAuthAPIPort));
		ras = new RpsApiSocket(new InetSocketAddress(config.rpsAPIAddress, config.rpsAPIPort));
		readBuffer = ByteBuffer.allocate(config.onionSize + OnionMessage.ONION_HEADER_SIZE);
		dataChannel = DatagramChannel.open().bind(new InetSocketAddress(config.onionDataPort));
		multiplexer = new Multiplexer(dataChannel, config.onionSize);
		oas = new OnionApiSocket(config.onionAPIPort);

		General.info("Waiting for Onion connections on " + config.onionPort + ".....");
		onionServerSocket = AsynchronousServerSocketChannel.open(config.group)
				.bind(new InetSocketAddress(config.onionAddress, config.onionPort));
		onionServerSocket.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
			@Override
			public void completed(AsynchronousSocketChannel channel, Void none) {
				onionServerSocket.accept(null, this);
				try {
					General.debug("Got connection from " + (InetSocketAddress) channel.getRemoteAddress() + " to "
							+ (InetSocketAddress) channel.getLocalAddress());
					new OnionSocket(multiplexer, channel);
				} catch (IOException e) {
					General.error("I/O error!");
					return;
				} catch (IllegalAddressException e) {
					General.warning("Got multiple TCP channel from one Hop!");
					return;
				}
				General.info("Onion connection successful");
			}

			@Override
			public void failed(Throwable exception, Void none) {
				General.fatal("Accept failed!");
			}
		});
		while (true) {
			InetSocketAddress addr = (InetSocketAddress) dataChannel.receive(readBuffer);
			OnionMessage message = OnionMessage.parse(readBuffer, OnionMessage.DATA_MESSAGE, addr);
			try {
				multiplexer.getReadQueue(message.id, message.address).offer(message);
			} catch (IllegalAddressException | IllegalIDException e) {
				General.warning("Got packet with wrong address or ID!");
			}
			readBuffer.clear();
		}
	}

	private static void parseArgs(String args[]) {
		if (args.length < 2 || !"-c".equals(args[0])) {
			System.out.println("Usage: java Main -c <path_to_config_file>");
			System.exit(1);
		}
		try {
			config = new Config(args[1]);
		} catch (InvalidKeyException e) {
			General.fatalException(e);
		} catch (InvalidFileFormatException e) {
			General.fatalException(e);
		} catch (IOException e) {
			General.fatalException(e);
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
