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

import static java.lang.Math.max;

import java.io.File;
import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.security.InvalidKeyException;
import java.security.interfaces.RSAPublicKey;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.ini4j.InvalidFileFormatException;
import org.ini4j.Wini;

import com.voidphone.general.General;

import util.PEMParser;

public class Config {
	// group for asynchronous channels
	public final AsynchronousChannelGroup group;
	// timeout for API connections
	public final int apiTimeout;
	// timeout for Onion connections
	public final int onionTimeout;
	// size of onion packets
	public final int onionSize;
	// address and port of the Onion Auth module API
	public final String onionAuthAPIAddress;
	public final short onionAuthAPIPort;
	// address and port of the RPS module API
	public final String rpsAPIAddress;
	public final short rpsAPIPort;
	// address and port of the Onion module API
	public final String onionAPIAddress;
	public final short onionAPIPort;
	// address and port of the Onion P2P
	public final String onionAddress;
	public final short onionPort;
	// path to hostkey
	public final String hostkeyPath;
	// hostkey of this peer
	public final RSAPublicKey hostkey;
	// Hop-count
	public final int hopCount;

	public Config(String configFilePath) throws InvalidFileFormatException, IOException, InvalidKeyException {
		Wini configFile = new Wini(new File(configFilePath));

		hostkeyPath = configFile.get("?", "HOSTKEY", String.class);

		hopCount = configFile.get("ONION", "hopcount", Integer.class).intValue();

		apiTimeout = configFile.get("ONION", "api_timeout", Integer.class).intValue();
		onionTimeout = configFile.get("ONION", "P2P_TIMEOUT", Integer.class).intValue();

		onionSize = configFile.get("ONION", "P2P_PACKETSIZE", Integer.class).intValue();

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
		onionAuthAPIAddress = apiAddressAndPort.substring(0, colonPos);
		onionAuthAPIPort = (short) Integer.parseInt(authAddressAndPort.substring(colonPos + 1));

		// RPS
		String rpsAddressAndPort = configFile.get("RPS", "api_address", String.class);
		colonPos = rpsAddressAndPort.lastIndexOf(':');
		rpsAPIAddress = rpsAddressAndPort.substring(0, colonPos);
		rpsAPIPort = (short) Integer.parseInt(rpsAddressAndPort.substring(colonPos + 1));

		File file = new File(hostkeyPath);
		hostkey = PEMParser.getPublicKeyFromPEM(file);

		int cores = Runtime.getRuntime().availableProcessors();
		ThreadFactory threadFactory = Executors.defaultThreadFactory();
		group = AsynchronousChannelGroup.withFixedThreadPool(max(1, cores - 1), threadFactory);

		General.debug("Hostkey: " + hostkeyPath + "; \nAPI will listen on " + onionAPIAddress + ", Port " + onionAPIPort
				+ "; \nOnion P2P will listen on " + onionAddress + ", Port " + onionPort + ". \n"
				+ "connecting to AUTH API on " + onionAuthAPIAddress + ":" + onionAuthAPIPort
				+ "; \nconnecting to RPS API on " + rpsAPIAddress + ":" + rpsAPIPort);
	}
}