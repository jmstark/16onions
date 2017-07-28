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

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Stub {
	private static String contains(BufferedReader out, String ident)
			throws IOException {
		for (;;) {
			String s = out.readLine();
			System.out.println(s);
			if (s.contains(ident)) {
				return s;
			}
		}
	}

	private static int getPort(BufferedReader out, String ident)
			throws IOException {
		String s = contains(out, ident);
		s = s.replaceAll("[^0-9]", "");
		return Integer.parseInt(s);
	}

	public static int getAPIPort(BufferedReader out) throws IOException {
		return getPort(out, "Waiting for API connection on ");
	}

	public static int getOnionPort(BufferedReader out) throws IOException {
		return getPort(out, "Waiting for Onion connections on ");
	}

	public static Socket getAPISocket(BufferedReader out) throws IOException {
		Socket s = new Socket();
		s.connect(new InetSocketAddress("127.0.0.1", Stub.getAPIPort(out)));
		return s;
	}

	public static Socket getOnionSocket(BufferedReader out, int port)
			throws IOException {
		Socket s = new Socket();
		s.connect(new InetSocketAddress("127.0.0.1", port));
		DataOutputStream dos = new DataOutputStream(s.getOutputStream());
		dos.writeInt(0x7af3bef1);
		dos.writeInt(1);
		dos.writeShort(16);
		dos.write(new byte[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15});
		contains(
				out,
				"Got connection from /" + s.getLocalAddress().getHostAddress() + ":"
						+ s.getLocalPort());
		DataInputStream dis = new DataInputStream(s.getInputStream());
		System.out.println(dis.readShort());
		System.out.println(dis.read());
		System.out.println(dis.read());
		System.out.println(dis.read());
		return s;
	}
}
