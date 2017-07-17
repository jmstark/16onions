package com.voidphone.api;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.InvalidKeyException;

import org.ini4j.Wini;
import com.voidphone.general.General;

import util.PEMParser;
public class Config {
	// Socket to the Onion Auth module API
	private OnionAuthAPISocket onionAuthAPISocket;
	// Socket to the RPS module API
	private RPSAPISocket rpsAPISocket;
	// addr + port of the Onion module API
	private String onionAPIAddress;
	private short onionAPIPort;
	
	// addr + port of the Onion P2P
	private String onionAddress;
	private short onionPort;
	private String hostkeyPath;
	private int hopCount;
	
	byte[] hostkey;
	
	
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
		String authAPIAddress = null;
		short authAPIPort = 0;
		String rpsAPIAddress = null;
		short rpsAPIPort = 0;
		
		try
		{
			Wini configFile = new Wini(new File(configFilePath));
			hostkeyPath = configFile.get("?", "HOSTKEY", String.class);
			
			hopCount = configFile.get("ONION", "hopcount", Integer.class).intValue();
			// api_address contains address and port, separated by a colon (':')
			String apiAddressAndPort = configFile.get("ONION", "api_address", String.class);
			int colonPos = apiAddressAndPort.lastIndexOf(':');
			onionAPIAddress = apiAddressAndPort.substring(0, colonPos);
			onionAPIPort = (short) Integer.parseInt(apiAddressAndPort.substring(colonPos + 1));
			// Onion hostname and port are separate config lines
			onionAddress = configFile.get("ONION", "P2P_HOSTNAME", String.class);
			onionPort = (short) configFile.get("ONION", "P2P_PORT", Integer.class).intValue();
			
			//OnionAuth
			String authAddressAndPort = configFile.get("AUTH", "api_address", String.class);
			colonPos = authAddressAndPort.lastIndexOf(':');
			authAPIAddress = apiAddressAndPort.substring(0, colonPos);
			authAPIPort = (short) Integer.parseInt(authAddressAndPort.substring(colonPos + 1));
			//RPS
			String rpsAddressAndPort = configFile.get("RPS", "api_address", String.class);
			colonPos = rpsAddressAndPort.lastIndexOf(':');
			rpsAPIAddress = rpsAddressAndPort.substring(0, colonPos);
			rpsAPIPort = (short) Integer.parseInt(rpsAddressAndPort.substring(colonPos + 1));
			
	        File file = new File(hostkeyPath);
	        hostkey = PEMParser.getPublicKeyFromPEM(file).getEncoded();

			
		}
		catch (IOException e)
		{
			General.fatal("FATAL: Could not read config file!");
			System.exit(1);
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			General.fatal("FATAL: Invalid key!");
			System.exit(1);
			e.printStackTrace();
		}
		// ugly dummy try catch
		try {
			onionAuthAPISocket = new OnionAuthAPISocket(new InetSocketAddress(authAPIAddress, authAPIPort));
			rpsAPISocket = new RPSAPISocket(new InetSocketAddress(rpsAPIAddress, rpsAPIPort));
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		General.debug("Hostkey: " + hostkeyPath + "; \nAPI will listen on " + onionAPIAddress + ", Port " 
				+ onionAPIPort + "; \nOnion P2P will listen on " + onionAddress + ", Port " + onionPort + ". \n" 
				+ "connecting to AUTH API on " + authAPIAddress + ":" + authAPIPort 
				+ "; \nconnecting to RPS API on " + rpsAPIAddress + ":" + rpsAPIPort);
	}
	
	public OnionAuthAPISocket getOnionAuthAPISocket() {
		return onionAuthAPISocket;
	}
	
	public RPSAPISocket getRPSAPISocket() {
		return rpsAPISocket;
	}
	
	public int getOnionAPIPort() {
		return onionAPIPort;
	}
	
	public int getOnionPort() {
		return onionPort;
	}
	
	public byte[] getHostkey() {
		return hostkey;
	}
	
	public int getHopCount() {
		return hopCount;
	}
}