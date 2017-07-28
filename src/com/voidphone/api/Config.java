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
package com.voidphone.api;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.security.InvalidKeyException;
import java.security.interfaces.RSAPublicKey;

import org.ini4j.Wini;

import com.voidphone.general.General;

import util.PEMParser;

public class Config {
	// Timeout for API connections
	private int apiTimeout;
	// Socket to the Onion Auth module API
	private OnionAuthApiSocket onionAuthAPISocket;
	// Socket to the RPS module API
	private RpsApiSocket rpsAPISocket;
	// addr + port of the Onion module API
	private String onionAPIAddress;
	private short onionAPIPort;

	// addr + port of the Onion P2P
	private String onionAddress;
	private short onionPort;
	private String hostkeyPath;
	private int hopCount;

	RSAPublicKey hostkey;

	public Config(String configFilePath) {
		readConfigValues(configFilePath);
	}

	/**
	 * Reads values from config file into variables
	 * 
	 * @param configFilePath
	 *            Path to the config file in INI format
	 */
	private void readConfigValues(String configFilePath) {
		String authAPIAddress = null;
		short authAPIPort = 0;
		String rpsAPIAddress = null;
		short rpsAPIPort = 0;

		try {
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

			// OnionAuth
			String authAddressAndPort = configFile.get("AUTH", "api_address", String.class);
			colonPos = authAddressAndPort.lastIndexOf(':');
			authAPIAddress = apiAddressAndPort.substring(0, colonPos);
			authAPIPort = (short) Integer.parseInt(authAddressAndPort.substring(colonPos + 1));
			// RPS
			String rpsAddressAndPort = configFile.get("RPS", "api_address", String.class);
			colonPos = rpsAddressAndPort.lastIndexOf(':');
			rpsAPIAddress = rpsAddressAndPort.substring(0, colonPos);
			rpsAPIPort = (short) Integer.parseInt(rpsAddressAndPort.substring(colonPos + 1));

			File file = new File(hostkeyPath);
			hostkey = PEMParser.getPublicKeyFromPEM(file);

		} catch (IOException e) {
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
			onionAuthAPISocket = new OnionAuthApiSocket(new InetSocketAddress(authAPIAddress, authAPIPort));
			rpsAPISocket = new RpsApiSocket(new InetSocketAddress(rpsAPIAddress, rpsAPIPort));

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		General.debug("Hostkey: " + hostkeyPath + "; \nAPI will listen on " + onionAPIAddress + ", Port " + onionAPIPort
				+ "; \nOnion P2P will listen on " + onionAddress + ", Port " + onionPort + ". \n"
				+ "connecting to AUTH API on " + authAPIAddress + ":" + authAPIPort + "; \nconnecting to RPS API on "
				+ rpsAPIAddress + ":" + rpsAPIPort);
	}

	public int getAPITimeout() {
		return apiTimeout;
	}
	
	public OnionAuthApiSocket getOnionAuthAPISocket() {
		return onionAuthAPISocket;
	}

	public RpsApiSocket getRPSAPISocket() {
		return rpsAPISocket;
	}

	public int getOnionApiPort() {
		return onionAPIPort;
	}

	public int getOnionPort() {
		return onionPort;
	}

	public byte[] getHostkey() {
		return hostkey.getEncoded();
	}

	public RSAPublicKey getHostkeyObject() {
		return hostkey;
	}

	public int getHopCount() {
		return hopCount;
	}
}