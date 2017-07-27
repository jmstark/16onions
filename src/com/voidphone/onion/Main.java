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
import java.util.Iterator;

import lombok.Getter;

import com.voidphone.api.Config;
import com.voidphone.api.OnionApiSocket;
import com.voidphone.api.OnionAuthApiSocket;
import com.voidphone.api.RpsApiSocket;
import com.voidphone.general.General;

import protocol.MessageParserException;
import protocol.ProtocolException;

public class Main {
	private static @Getter Selector selector;
	private static Config config;
	private static @Getter OnionApiSocket oas;

	private static void run2() throws IOException {
		final DatagramChannel dataChannel;
		final OnionApiSocket onionApiSocket;
		final AsynchronousServerSocketChannel onionServerSocket;
		final Multiplexer multiplexer;
		final ByteBuffer readBuffer;

		multiplexer = new Multiplexer();
		General.info("Waiting for API connection on " + config.getOnionApiPort() + ".....");
		onionApiSocket = new OnionApiSocket(config.getOnionApiPort());
		General.debug("API connection successful");
		General.info("Waiting for Onion connections on " + config.getOnionPort() + ".....");
		onionServerSocket = AsynchronousServerSocketChannel.open().bind(new InetSocketAddress(config.getOnionPort()));
		onionServerSocket.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
			@Override
			public void completed(AsynchronousSocketChannel channel, Void none) {
				onionServerSocket.accept(null, this);
				try {
					OnionSocket onionSocket = new OnionSocket(multiplexer, channel);
					// TODO: monitor and delete unused OnionSocket
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			@Override
			public void failed(Throwable exception, Void none) {
				// TODO Auto-generated method stub
			}
		});
		readBuffer = ByteBuffer.allocate(2 * Short.MAX_VALUE);
		dataChannel = DatagramChannel.open().bind(new InetSocketAddress(1234));
		while (true) {
			dataChannel.receive(readBuffer);
			OnionMessage message = OnionMessage.parse(readBuffer);
			try {
				multiplexer.getQueue(message.getId()).offer(message);
			} catch (IllegalArgumentException e) {
				// TODO: kill connection
			}
			readBuffer.clear();
		}
	}

	/**
	 * Runs the Onion module.
	 * 
	 * @throws Exception
	 */
	private static void run() throws Exception {
		try {
			// socket for all incoming UDP packets,
			// for OnionConnectingSocket as well as OnionListenerSocket
			DatagramChannel udpChannel = DatagramChannel.open()
					.bind(new InetSocketAddress("127.0.0.1", config.getOnionPort()));

			selector = Selector.open();
			General.info("Waiting for API connection on " + config.getOnionApiPort() + ".....");
			SocketChannel onionApiSocket = ServerSocketChannel.open()
					.bind(new InetSocketAddress("127.0.0.1", config.getOnionApiPort())).accept();
			General.debug("API connection successful");
			General.info("Waiting for Onion connections on " + config.getOnionPort() + ".....");
			ServerSocketChannel onionServerSocket = ServerSocketChannel.open()
					.bind(new InetSocketAddress("127.0.0.1", config.getOnionPort()));

			// for API requests
			oas = new OnionApiSocket(onionApiSocket, config);
			onionApiSocket.configureBlocking(false);
			onionApiSocket.register(selector, SelectionKey.OP_READ, oas);

			// for incoming Onion connections
			onionServerSocket.configureBlocking(false);
			onionServerSocket.register(selector, SelectionKey.OP_ACCEPT);

			// wait for any socket getting ready
			while (selector.select() != 0) {
				Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
				while (iterator.hasNext()) {
					SelectionKey key = iterator.next();
					if (key.isAcceptable()) {
						// OnionServerSocket got a connection request
						General.debug("Onion connection requested.....");
						SocketChannel onionSocket = onionServerSocket.accept();
						General.debug("Onion connection successful");
						// create a new OnionListenerSocket ...
						OnionListenerSocket ols = new OnionListenerSocket(onionSocket.socket(), config);
						General.debug("Got connection from " + onionSocket.getRemoteAddress());
						onionSocket.configureBlocking(false);
						// ... and add it to the selector
						onionSocket.register(selector, SelectionKey.OP_READ, ols);
					} else if (key.isReadable()) {
						// an OnionListenerSocket or the OnionAPISocket received
						// data
						General.debug("Received packet");
						// enable blocking
						key.cancel();
						selector.selectNow();
						key.channel().configureBlocking(true);
						// handle the received data
						boolean again = ((Attachable) key.attachment()).handle();
						if (!again) {
							// the connection is still alive
							key.channel().configureBlocking(false);
							key.channel().register(selector, SelectionKey.OP_READ, key.attachment());
						}
					} else {
						General.fatal("Selector returns unknown key!");
					}
				}
				selector.selectedKeys().clear();
			}
		} catch (IOException | MessageParserException | ProtocolException e) {
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
	public static void main(String[] args) throws Exception {
		General.initDebugging();

		parseArgs(args);

		run();
	}

	public static interface Attachable {
		public boolean handle() throws Exception, IOException, MessageParserException, ProtocolException;
	}
}
