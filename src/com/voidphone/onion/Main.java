package com.voidphone.onion;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import com.voidphone.api.Config;
import com.voidphone.api.OnionAPISocket;
import com.voidphone.api.OnionAuthAPISocket.AUTHSESSIONHS1;
import com.voidphone.api.RPSAPISocket.OnionPeer;
import com.voidphone.general.General;

public class Main {
	private static Config config;

	/**
	 * Runs the Onion module.
	 */
	private static void run() {
		try {
			Selector selector = Selector.open();
			General.info("Waiting for API connection on "
					+ config.getOnionAPIPort() + ".....");
			SocketChannel onionAPISocket = ServerSocketChannel
					.open()
					.bind(new InetSocketAddress("127.0.0.1", config
							.getOnionAPIPort())).accept();
			General.debug("API connection successful");
			General.info("Waiting for Onion connections on "
					+ config.getOnionPort() + ".....");
			ServerSocketChannel onionServerSocket = ServerSocketChannel.open()
					.bind(new InetSocketAddress("127.0.0.1", config
							.getOnionPort()));

			// for API requests
			OnionAPISocket oas = new OnionAPISocket(onionAPISocket, config);
			onionAPISocket.configureBlocking(false);
			onionAPISocket.register(selector, SelectionKey.OP_READ, oas);

			// for incoming Onion connections
			onionServerSocket.configureBlocking(false);
			onionServerSocket.register(selector, SelectionKey.OP_ACCEPT);

			// wait for any socket getting ready
			while (selector.select() != 0) {
				Iterator<SelectionKey> iterator = selector.selectedKeys()
						.iterator();
				while (iterator.hasNext()) {
					SelectionKey key = iterator.next();
					if (key.isAcceptable()) {
						// OnionServerSocket got a connection request
						General.debug("Onion connection requested.....");
						SocketChannel onionSocket = onionServerSocket.accept();
						General.debug("Onion connection successful");
						// create a new OnionListenerSocket ...
						OnionListenerSocket ols = new OnionListenerSocket(
								onionSocket.socket(), config);
						General.debug("Got connection from "
								+ onionSocket.getRemoteAddress());
						onionSocket.configureBlocking(false);
						// ... and add it to the selector
						onionSocket.register(selector, SelectionKey.OP_READ,
								ols);
					} else if (key.isReadable()) {
						// an OnionListenerSocket or the OnionAPISocket received
						// data
						General.debug("Received packet");
						// enable blocking
						key.cancel();
						selector.selectNow();
						key.channel().configureBlocking(true);
						// handle the received data
						// TODO: handle IOExcpetion thrown by handle()
						if (!((Attachable) key.attachment()).handle()) {
							// the connection is still alive
							key.channel().configureBlocking(false);
							key.channel().register(selector,
									SelectionKey.OP_READ, key.attachment());
						}
					} else {
						General.fatal("Selector returns unknown key!");
					}
				}
				selector.selectedKeys().clear();
			}
		} catch (Exception e) {
			General.fatalException(e);
		}
	}

	private static void parseArgs(String args[]) {
		if (args.length < 2 || !"-c".equals(args[0])) {
			System.out.println("Usage: java Main -c <path_to_config_file>");
			System.exit(1);
		}
		config = new Config(args[1]);
	}

	/**
	 * @param args
	 *            Command line arguments
	 * 
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		General.initDebugging();

		parseArgs(args);

		run();
	}

	public static interface Attachable {
		public boolean handle() throws IOException;
	}
}
