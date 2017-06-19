package com.voidphone.api;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.ini4j.Wini;

public class Config {
	private String apiAddress;
	private int apiPort;
	// Socket to the Onion Auth module API
	private OnionAuthAPISocket onionAuthAPISocket;
	// Socket to the RPS module API
	private RPSAPISocket RPSAPISocket;
	// port of the Onion module API
	private int onionAPIPort;
	private String onionAddress;
	private int onionPort;
	private String hostkeyPath;
	
	
	public Config(String configFilePath)
	{
		readConfigValues(configFilePath);
	}
	
	/**
	 * Reads values from config file into variables
	 * 
	 * @param configFilePath
	 *            Path to the config file in INI format
	 */
	private void readConfigValues(String configFilePath)
	{
		onionAuthAPISocket = new OnionAuthAPISocket();
		onionAPIPort = 30000;
		onionPort = 30001;
/*		try
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
			System.err.println("FATAL: Could not read config file!!!");
			System.exit(1);
		}*/
		System.out.println("DEBUG: Hostkey: " + hostkeyPath + "; API will listen on " + apiAddress + ", Port " + apiPort
				+ "; Onion will listen on " + onionAddress + ", Port " + onionPort + ". ");
	}
	
	public OnionAuthAPISocket getOnionAuthAPISocket() {
		return onionAuthAPISocket;
	}
	
	public RPSAPISocket getRPSAPISocket() {
		return RPSAPISocket;
	}
	
	public int getOnionAPIPort() {
		return onionAPIPort;
	}
	
	public int getOnionPort() {
		return onionPort;
	}
}
