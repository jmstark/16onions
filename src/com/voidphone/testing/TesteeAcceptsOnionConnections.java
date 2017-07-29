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

import java.net.InetSocketAddress;
import java.net.Socket;

import com.voidphone.testing.TestProcess;

public class TesteeAcceptsOnionConnections {
	public static void main(String args[]) throws Exception {
		Helper.generateConfig(1);
		TestProcess p = new TestProcess(com.voidphone.onion.Main.class, Helper.classpath,
				new String[] { "-c", Helper.getConfigPath(0) });
		Socket api = new Socket();
		Helper.contains(p.getOut(), "Waiting for API connection on ");
		api.connect(Helper.getAddressFromConfig(Helper.getPeerConfig(0), "onion", "api_address"));
		Socket onion = new Socket();
		Helper.contains(p.getOut(), "Waiting for Onion connections on ");
		onion.connect(new InetSocketAddress("127.0.0.1",
				Helper.getPeerConfig(0).config.get("onion", "p2p_port", Integer.class).intValue()));
		Helper.contains(p.getOut(), "Onion connection successful");
		p.terminate();
		api.close();
		onion.close();
	}
}
