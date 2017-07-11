package com.voidphone.onion;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import com.voidphone.api.Config;
import com.voidphone.api.OnionAPISocket;
import com.voidphone.api.RPSAPISocket.RPSPEER;
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
	 */	 
	private static void constructTunnel(byte[] targetAddress, short targetPort, byte[] targetHostkey, int numHops) throws Exception
	{
		//TODO: we only need either InetSocketAddress or address+port,
		//but let's leave this until we're sure of which one.
		byte[][] hopAddress = new byte[numHops + 1][];
		short[] hopPort = new short[numHops + 1];
		byte[][] hopHostkey = new byte[numHops + 1][];
		Socket[] hopSocket = new Socket[numHops + 1];
		InetSocketAddress[] hopInetSocketAddress = new InetSocketAddress[numHops + 1];
		OnionConnectingSocket[] onionSockets = new OnionConnectingSocket[numHops+1];
		if(targetAddress!=null)
		{
			hopAddress[numHops] = targetAddress;
			hopPort[numHops] = targetPort;
			hopHostkey[numHops] = targetHostkey;
		}
		for(int i=0;i <= numHops;i++)
		{
			if(i>0)
			{
				//request forwarding
				config.getOnionAuthAPISocket().encryptData(data);
				//onionSockets[i-1].
			}
			
			//If destination == null, chose randomly
			if(hopAddress[i] == null)
			{
				RPSPEER newHop = config.getRPSAPISocket().RPSQUERY();
				hopAddress[i] = newHop.getAddress().getAddress().getAddress();
				hopPort[i] = (short) newHop.getAddress().getPort();
				hopHostkey[i] = newHop.getHostkey();
				hopInetSocketAddress[i] = newHop.getAddress();
			}
			hopSocket[i] = new Socket(hopInetSocketAddress[i].getAddress(),hopInetSocketAddress[i].getPort());
			onionSockets[i] = new OnionConnectingSocket(hopSocket[i].getChannel(), hopHostkey[i], config);
		}
			

	}


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

			OnionAPISocket oas = new OnionAPISocket(onionAPISocket);
			onionAPISocket.configureBlocking(false);
			onionAPISocket.register(selector, SelectionKey.OP_READ, oas);

			onionServerSocket.configureBlocking(false);
			onionServerSocket.register(selector, SelectionKey.OP_ACCEPT);

			while (selector.select() != 0) {
				Iterator<SelectionKey> iterator = selector.selectedKeys()
						.iterator();
				while (iterator.hasNext()) {
					SelectionKey key = iterator.next();
					if (key.isAcceptable()) {
						General.debug("Onion connection requested.....");
						SocketChannel onionSocket = onionServerSocket.accept();
						General.debug("Onion connection successful");
						OnionListenerSocket ols = new OnionListenerSocket(
								onionSocket, null, config);
						General.debug("Got connection from "
								+ onionSocket.getRemoteAddress());
						onionSocket.configureBlocking(false);
						onionSocket.register(selector, SelectionKey.OP_READ,
								ols);
					} else if (key.isReadable()) {
						General.debug("Received packet");
						key.cancel();
						selector.selectNow();
						key.channel().configureBlocking(true);
						if (!((Attachable) key.attachment()).handle()) {
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
		public boolean handle();
	}
}
