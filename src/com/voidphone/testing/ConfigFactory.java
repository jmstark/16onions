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
package com.voidphone.testing;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.ini4j.Wini;

import com.voidphone.general.General;

public class ConfigFactory {
	private static final int max_connections = 30;
	private static final int cache_size = 50;
	public final Wini config;
	private final String name;
	private int port;

	public ConfigFactory(String name, String bootstrapper, int port) {
		this.port = port;
		this.name = name;
		this.config = new Wini();
		config.add("onion", "hostkey", "");
		if (bootstrapper == null) {
			bootstrapper = nextPort();
			config.add("gossip", "listen_address", bootstrapper);
		} else {
			config.add("gossip", "listen_address", nextPort());
		}
		config.add("gossip", "bootstrapper", bootstrapper);
		config.add("gossip", "api_address", nextPort());
		config.add("gossip", "max_connections", max_connections);
		config.add("gossip", "cache_size", cache_size);
		config.add("rps", "api_address", nextPort());
		config.add("rps", "listen_address", nextPort());
		config.add("onion", "api_address", nextPort());
		config.add("onion", "listen_address", nextPort());
		config.add("onion", "api_timeout", 3000);
		config.add("onion", "cache_size", 10);
		config.add("onion", "hopcount", 3);
		config.add("onion", "p2p_hostname", "xxx");
		config.add("onion", "p2p_port", this.port);
		this.port++;
		config.add("onion", "p2p_data_port", this.port);
		this.port++;
		config.add("onion", "p2p_timeout", 5000);
		config.add("onion", "p2p_packetsize", 3);
		config.add("auth", "api_address", nextPort());
		config.add("auth", "listen_address", nextPort());
	}

	public String store(Path path) throws IOException, InterruptedException {
		path = Paths.get(path.toAbsolutePath().toString(), name);
		path.toFile().mkdirs();

		String hostkeyPath = Paths.get(path.toString(), name + ".pem").toString();
		Path configPath = Paths.get(path.toString(), name + ".conf");

		String cmd[] = new String[] { "openssl", "genrsa", "-out", hostkeyPath, "4096" };
		if (new ProcessBuilder(cmd).start().waitFor() != 0) {
			General.fatal("Hostkey generation failed!");
		}
		config.put("onion", "hostkey", hostkeyPath);

		config.store(configPath.toFile());
		return configPath.toString();
	}

	private String nextPort() {
		if (port >= Short.MAX_VALUE) {
			General.fatal("Port is too large!");
		}
		String ret = "127.0.0.1:" + port;
		port++;
		return ret;
	}
}
