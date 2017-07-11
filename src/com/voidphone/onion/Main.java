package com.voidphone.onion;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import com.voidphone.api.Config;
import com.voidphone.api.OnionAPISocket;
import com.voidphone.general.General;

public class Main {
	private static Config config;

	/**
	 * Constructs iteratively a tunnel to the target. If targetAddress is null,
	 * a random target node is selected.
	 * 
	 * @param targetAddress
	 * @param targetPort
	 * @param targetHostkey
	 * @param numHops
	 * @throws Exception
	 * 
	 *             private void constructTunnel(byte[] targetAddress, short
	 *             targetPort, byte[] targetHostkey, int numHops) throws
	 *             Exception { final int bufsize=12345; byte[][] hopAddress =
	 *             new byte[numHops + 1][]; short[] hopPort = new short[numHops
	 *             + 1]; byte[][] hopHostkey = new byte[numHops + 1][];
	 *             OnionConnectingSocket[] onionSockets = new
	 *             OnionConnectingSocket[numHops+1]; if(targetAddress!=null) {
	 *             hopAddress[numHops] = targetAddress; hopPort[numHops] =
	 *             targetPort; hopHostkey[numHops] = targetHostkey; } for(int
	 *             i=0;i <= numHops;i++) { if(hopAddress[i] == null) { //TODO:
	 *             RPS-query -> hopAddress, hopPort, hopHostkey } } Socket
	 *             nextHop = new
	 *             Socket(InetAddress.getByAddress(hopAddress[0]),hopPort[0]);
	 *             onionSockets[0] = new OnionConnectingSocket(nextHop,
	 *             hopHostkey[0], config);
	 * 
	 * 
	 *             }
	 */

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
			OnionAPISocket oas = new OnionAPISocket(onionAPISocket);
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
						OnionMiddle om = new OnionMiddle(onionSocket, config);
						General.debug("Got connection from "
								+ onionSocket.getRemoteAddress());
						onionSocket.configureBlocking(false);
						// ... and add it to the selector
						onionSocket
								.register(selector, SelectionKey.OP_READ, om);
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
