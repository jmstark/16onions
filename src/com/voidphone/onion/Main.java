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

public class Main
{
	private static Config config;

	/**
	 * Constructs iteratively a tunnel to the target.
	 * If targetAddress is null, a random target node is selected.
	 * @param targetAddress
	 * @param targetPort
	 * @param targetHostkey
	 * @param numHops
	 * @throws Exception
	 *
	private void constructTunnel(byte[] targetAddress, short targetPort, byte[] targetHostkey, int numHops) throws Exception
	{
		final int bufsize=12345;
		byte[][] hopAddress = new byte[numHops + 1][];
		short[] hopPort = new short[numHops + 1];
		byte[][] hopHostkey = new byte[numHops + 1][];
		OnionConnectingSocket[] onionSockets = new OnionConnectingSocket[numHops+1];
		if(targetAddress!=null)
		{
			hopAddress[numHops] = targetAddress;
			hopPort[numHops] = targetPort;
			hopHostkey[numHops] = targetHostkey;
		}
		for(int i=0;i <= numHops;i++)
		{
			if(hopAddress[i] == null)
			{
				//TODO: RPS-query -> hopAddress, hopPort, hopHostkey
			}
		}
		Socket nextHop = new Socket(InetAddress.getByAddress(hopAddress[0]),hopPort[0]);
		onionSockets[0] = new OnionConnectingSocket(nextHop, hopHostkey[0], config);
		

	}*/

	

	/**
	 * Runs the Onion module.
	 */
	private static void run()
	{
		try {
			Selector selector = Selector.open();
			System.out.println("DEBUG: Waiting for API connection on "+config.getOnionAPIPort()+".....");
			SocketChannel onionAPISocket = ServerSocketChannel.open().bind(new InetSocketAddress("127.0.0.1",config.getOnionAPIPort())).accept();
			System.out.println("DEBUG: API connection successful");
			System.out.println("DEBUG: Waiting for Onion connections on "+config.getOnionPort()+".....");
			ServerSocketChannel onionServerSocket = ServerSocketChannel.open().bind(new InetSocketAddress("127.0.0.1",config.getOnionPort()));
			
			OnionAPISocket oas = new OnionAPISocket(onionAPISocket);
			onionAPISocket.configureBlocking(false);
			onionAPISocket.register(selector, SelectionKey.OP_READ, oas);
			
			onionServerSocket.configureBlocking(false);
			onionServerSocket.register(selector, SelectionKey.OP_ACCEPT);
			
			while (selector.select() != 0) {
				Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
				while (iterator.hasNext()) {
					SelectionKey key = iterator.next();
					if (key.isAcceptable()) {
						System.out.println("DEBUG: Onion connection requested.....");
						SocketChannel onionSocket = onionServerSocket.accept();
						System.out.println("DEBUG: Onion connection successful");
						OnionListenerSocket ols = new OnionListenerSocket(onionSocket, null, config);
						onionSocket.configureBlocking(false);
						onionSocket.register(selector, SelectionKey.OP_READ, ols);
					} else if (key.isReadable()) {
						System.out.println("DEBUG: Received packet");
						key.cancel();
						selector.selectNow();
						key.channel().configureBlocking(true);
						if (!((Attachable)key.attachment()).handle()) {
							key.channel().configureBlocking(false);
							key.channel().register(selector, SelectionKey.OP_READ, key.attachment());
						}
					} else {
						System.err.println("FATAL: Selector returns unknown key!!!");
						System.exit(1);
					}
					iterator.remove();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
	private static void parseArgs(String args[]) {
		if (args.length < 2 || !"-c".equals(args[0]))
		{
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
	public static void main(String[] args) throws IOException
	{
		parseArgs(args);

		run();
	}
	
	public static interface Attachable {
		public boolean handle();
	}
}
