package com.voidphone.onion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
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

import sun.net.InetAddressCachePolicy;


public class Main
{

	private String apiAddress;
	private int apiPort;
	private String onionAddress;
	private int onionPort;
	private String hostKeyPath;
	
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
	 * @param configFilePath Path to the config file in INI format
	 */
	private void readConfigValues(String configFilePath)
	{
		try
		{
			
			Wini configFile = new Wini(new File(configFilePath));
			Set<String> sectionNames = configFile.keySet();
			hostKeyPath = configFile.get("?", "HOSTKEY", String.class);
			
			//api_address contains address and port, separated by a colon (':')
			String apiAddressAndPort = configFile.get("ONION","api_address",String.class);
			int colonPos = apiAddressAndPort.lastIndexOf(':');
			apiAddress = apiAddressAndPort.substring(0, colonPos);
			apiPort = Integer.parseInt(apiAddressAndPort.substring(colonPos + 1));
			
			//Onion hostname and port are separate config lines
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
	 * Runs the Onion module.
	 * 
	 * @param configFilePath Path to the config file in INI format
	 */
	private void run(String configFilePath) throws IOException
	{
		
		readConfigValues(configFilePath);
		
		System.out.println("Hostkey: " + hostKeyPath + "; API will listen on "
				+ apiAddress + ", Port " + apiPort  + "; Onion will listen on " 
				+ onionAddress + ", Port " + onionPort + ". ");
		
		
		//run onion socket accepting onion connections from other peers here
		//in another thread? i think so.
		
		
		//run the API-loop				
		
		
		// from https://www.cs.uic.edu/~troy/spring05/cs450/sockets/EchoServer.java
		ServerSocket apiServerSocket = null;

		try
		{
			apiServerSocket = new ServerSocket();
			apiServerSocket.bind(new InetSocketAddress(apiAddress, apiPort));
		}
		catch (IOException e)
		{
			System.err.println("Could not listen on " + apiAddress + ':' + apiPort);
			System.exit(1);
		}

		Socket clientSocket = null;
		System.out.println("Waiting for connection.....");

		try
		{
			clientSocket = apiServerSocket.accept();
		}
		catch (IOException e)
		{
			System.err.println("Accept failed.");
			System.exit(1);
		}

		System.out.println("Connection successful");
		System.out.println("Waiting for input.....");

		PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
		BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

		String inputLine;

		while ((inputLine = in.readLine()) != null)
		{
			System.out.println("Server: " + inputLine);
			out.println(inputLine);

			if (inputLine.equals("Bye."))
				break;
		}

		out.close();
		in.close();
		clientSocket.close();
		apiServerSocket.close();
	}

	/**
	 * @param args Command line arguments
	 * 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException
	{
		if(args.length < 2 || !"-c".equals(args[0]))
		{
			System.out.println("Usage: java Main -c <path_to_config_file>");	
			System.exit(1);
		}		
		
		new Main().run(args[1]);
	}
}
