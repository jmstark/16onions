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


/**
 * This class reads all neccesary config values from an INI-file.
 *
 */
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
	public final int onionAuthAPIPort;
	// address and port of the RPS module API
	public final String rpsAPIAddress;
	public final int rpsAPIPort;
	// address and port of the Onion module API
	public final String onionAPIAddress;
	public final int onionAPIPort;
	// address and port of the Onion P2P
	public final String onionAddress;
	public final int onionPort;
	// hostkey of this peer
	public final RSAPublicKey hostkey;
	// Hop-count
	public final int hopCount;
	// round time in milliseconds
	public final long roundtime;
	// how many milliseconds before the next round we start preparing (e.g. backup tunnels)
	public final long roundPrepareTime;

	public Config(String configFilePath) throws InvalidFileFormatException, IOException, InvalidKeyException {
		Wini configFile = new Wini();
		org.ini4j.Config iniConfig = configFile.getConfig();
		iniConfig.setLowerCaseOption(true);
		iniConfig.setLowerCaseSection(true);
		configFile.setConfig(iniConfig);
		configFile.load(new File(configFilePath));

		String hostkeyPath = configFile.get("?", "hostkey", String.class);
		if (hostkeyPath == null) {
			hostkeyPath = getString(configFile, "onion", "hostkey");
		}

		// api_address contains address and port, separated by a colon (':')
		String apiAddressAndPort = getString(configFile, "onion", "api_address");
		int colonPos = apiAddressAndPort.lastIndexOf(':');
		onionAPIAddress = apiAddressAndPort.substring(0, colonPos);
		onionAPIPort = Integer.parseInt(apiAddressAndPort.substring(colonPos + 1));

		// OnionAuth
		String authAddressAndPort = getString(configFile, "auth", "api_address");
		colonPos = authAddressAndPort.lastIndexOf(':');
		onionAuthAPIAddress = apiAddressAndPort.substring(0, colonPos);
		onionAuthAPIPort = Integer.parseInt(authAddressAndPort.substring(colonPos + 1));

		// RPS
		String rpsAddressAndPort = getString(configFile, "rps", "api_address");
		colonPos = rpsAddressAndPort.lastIndexOf(':');
		rpsAPIAddress = rpsAddressAndPort.substring(0, colonPos);
		rpsAPIPort = Integer.parseInt(rpsAddressAndPort.substring(colonPos + 1));

		File file = new File(hostkeyPath);
		hostkey = PEMParser.getPublicKeyFromPEM(file);

		int cores = Runtime.getRuntime().availableProcessors();
		ThreadFactory threadFactory = Executors.defaultThreadFactory();
		group = AsynchronousChannelGroup.withFixedThreadPool(max(1, cores - 1), threadFactory);

		// Onion hostname and port are separate config lines
		String onionAddressAndPort = getString(configFile, "onion", "listen_address");
		colonPos = onionAddressAndPort.lastIndexOf(':');
		onionAddress = onionAddressAndPort.substring(0, colonPos);
		onionPort = Integer.parseInt(onionAddressAndPort.substring(colonPos + 1));

		hopCount = getInteger(configFile, "onion", "hopcount");
		apiTimeout = getInteger(configFile, "onion", "api_timeout");
		onionTimeout = getInteger(configFile, "onion", "p2p_timeout");
		onionSize = getInteger(configFile, "onion", "p2p_packetsize");
		roundtime = getInteger(configFile, "onion", "roundtime");
		roundPrepareTime = getInteger(configFile, "onion", "round_prepare_time");

		General.debug("Hostkey: " + hostkeyPath);
	}

	private int getInteger(Wini configFile, String section, String option) {
		Integer bp = configFile.get(section, option, Integer.class);
		if (bp == null) {
			General.fatal("Could not find " + option + " in " + section + "!");
			return 0;
		} else {
			return bp.intValue();
		}
	}

	private String getString(Wini configFile, String section, String option) {
		String bp = configFile.get(section, option, String.class);
		if (bp == null) {
			General.fatal("Could not find option \"" + option + "\" in section \"" + section + "\"!");
		}
		return bp;
	}
}