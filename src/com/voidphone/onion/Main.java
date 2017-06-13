package com.voidphone.onion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Set;

import org.ini4j.InvalidFileFormatException;
import org.ini4j.Wini;

import com.voidphone.api.APISocket;

import sun.net.InetAddressCachePolicy;

public class Main
{

	//for tunnel building, we don't know if its IPv4 or IPv6 addresses
	//TODO: remove this variable and find it out for each message
	public final int ipAddressLength = 4;
	private String apiAddress;
	private int apiPort;
	private String onionAddress;
	private int onionPort;
	private String hostkeyPath;

	/**
	 * Returns one.
	 * 
	 * @return always one.
	 */
	public static int one()
	{
		return 1;
	}

	/**
	 * Reads values from config file into variables
	 * 
	 * @param configFilePath
	 *            Path to the config file in INI format
	 */
	private void readConfigValues(String configFilePath)
	{
		try
		{

			Wini configFile = new Wini(new File(configFilePath));
			hostkeyPath = configFile.get("?", "HOSTKEY", String.class);

			// api_address contains address and port, separated by a colon (':')
			String apiAddressAndPort = configFile.get("ONION", "api_address", String.class);
			int colonPos = apiAddressAndPort.lastIndexOf(':');
			apiAddress = apiAddressAndPort.substring(0, colonPos);
			apiPort = Integer.parseInt(apiAddressAndPort.substring(colonPos + 1));

			// Onion hostname and port are separate config lines
			onionAddress = configFile.get("ONION", "P2P_HOSTNAME", String.class);
			onionPort = configFile.get("ONION", "P2P_PORT", Integer.class).intValue();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Constructs iteratively a tunnel to the target.
	 * If targetAddress is null, a random target node is selected.
	 * @param targetAddress
	 * @param targetPort
	 * @param targetHostkey
	 * @param numHops
	 * @throws Exception
	 */
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
		onionSockets[0] = new OnionConnectingSocket(nextHop, hopHostkey[0], bufsize);
		

	}

	/**
	 * Listens for incoming TCP API connection, accepts API requests, unpacks
	 * them and calls the appropriate methods, and sends answers (if
	 * applicable).
	 * Needs to process 
	 * ONION_TUNNEL_BUILD, ONION_TUNNEL_DESTROY, ONION_TUNNEL_DATA, ONION_COVER
	 * 
	 * @throws IOException
	 */
	private void runApiListener()
	{
		while (true)
		{
			ServerSocket apiServerSocket = null;
			Socket clientSocket = null;
			DataInputStream in = null;
			
			try
			{
				apiServerSocket = new ServerSocket();
				apiServerSocket.bind(new InetSocketAddress(apiAddress, apiPort));
				System.out.println("Waiting for API connection.....");
				clientSocket = apiServerSocket.accept();
				System.out.println("API Connection successful");
				System.out.println("Waiting for API input.....");
				//BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				in = new DataInputStream(clientSocket.getInputStream());

				boolean exitLoop = false;
				while(!exitLoop)
				{
					short msgLength = in.readShort();
					if(msgLength < 8)
					{
						throw new IOException("API message too short: " + msgLength + "Bytes.");
					}
					short msgType = in.readShort();
					switch(msgType)
					{
					case APISocket.MSG_TYPE_ONION_TUNNEL_BUILD:
						//skip reserved 2 bytes
						in.readShort();
						int targetPort = in.readShort();
						byte[] targetIpAddress = new byte[ipAddressLength];
						in.readFully(targetIpAddress, 0, targetIpAddress.length);
						int hostkeyLength = msgLength - (8 + targetIpAddress.length);
						if(hostkeyLength <= 0)
							throw new IOException("API message or target DER-hostkey too short");
						byte[] targetHostkey = new byte[hostkeyLength];
						in.readFully(targetHostkey, 0, targetHostkey.length);
						//TODO: call function with the now unpacked arguments.
						// e.g. onionTunnelBuild(targetIpAddress,targetPort,targetHostkey);
						break;
					case APISocket.MSG_TYPE_ONION_TUNNEL_DESTROY:
						int tunnelId = in.readInt();
						//TODO: call function with the now unpacked arguments.
						break;
					case APISocket.MSG_TYPE_ONION_TUNNEL_DATA:
						tunnelId = in.readInt();
						int dataLength = msgLength - 8;
						if(dataLength <= 0)
							throw new IOException("API message or data too short");
						byte[] data = new byte[dataLength];
						in.readFully(data, 0, dataLength);
						//TODO: call function with the now unpacked arguments.
						break;
					case APISocket.MSG_TYPE_ONION_COVER:
						short coverSize = in.readShort();
						//skip reserved 2 bytes
						in.readShort();
						//TODO: call function with the now unpacked arguments.
						break;
					}
					
				}
				
				in.close();
				clientSocket.close();
				apiServerSocket.close();
				

			}
			catch (IOException e)
			{
				System.out.println("API connection lost: " + e.getMessage());
			}
		}
	}

	/**
	 * Runs the Onion module.
	 * 
	 * @param configFilePath
	 *            Path to the config file in INI format
	 */
	private void run(String configFilePath) throws IOException
	{

		readConfigValues(configFilePath);

		System.out.println("Hostkey: " + hostkeyPath + "; API will listen on " + apiAddress + ", Port " + apiPort
				+ "; Onion will listen on " + onionAddress + ", Port " + onionPort + ". ");

		// run onion socket accepting onion connections from other peers here
		// in another thread? i think so.

		// run the API-loop
		runApiListener();
	}

	/**
	 * @param args
	 *            Command line arguments
	 * 
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException
	{
		if (args.length < 2 || !"-c".equals(args[0]))
		{
			System.out.println("Usage: java Main -c <path_to_config_file>");
			System.exit(1);
		}

		new Main().run(args[1]);
	}
}
