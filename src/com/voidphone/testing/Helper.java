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
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Random;

import com.voidphone.general.General;
import com.voidphone.onion.OnionMessage;

public class Helper {
	public static final String classpath[] = new String[] {
			System.getProperty("user.dir") + "/testing/libs/commons-cli-1.3.1.jar",
			System.getProperty("user.dir") + "/testing/libs/ini4j-0.5.4.jar", "junit-4.12.jar",
			System.getProperty("user.dir") + "/testing/libs/bcprov-jdk15on-155.jar" };
	private static final boolean deleteConfigAfterTest = true;
	private static final HashMap<Integer, ConfigFactory> peers = new HashMap<Integer, ConfigFactory>();
	private static final Path configs = Paths.get(System.getProperty("user.dir"), "tmp");

	public static String getConfigPath(int peer) {
		return configs.toString() + "/peer" + peer + "/peer" + peer + ".conf";
	}

	public static ConfigFactory getPeerConfig(int peer) {
		return peers.get(peer);
	}

	public static String generateConfig(int number) throws IOException, InterruptedException {
		if (configs.toFile().exists()) {
			General.fatal(configs.toString() + " exists. Please delete it.");
		}
		int port = new Random().nextInt(Short.MAX_VALUE - 1024) + 1024;
		if (number <= 0) {
			throw new IllegalArgumentException("Number of peers is <= 0!");
		}
		String bootstrapper = "127.0.0.1:" + port;
		ConfigFactory config = new ConfigFactory("peer0", null, port);
		config.store(configs);
		peers.put(0, config);
		for (int i = 1; i < number; i++) {
			port += 64;
			config = new ConfigFactory("peer" + i, bootstrapper, port);
			config.store(configs);
			peers.put(i, config);
		}
		if (deleteConfigAfterTest) {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					try {
						Files.walkFileTree(configs, new SimpleFileVisitor<Path>() {
							@Override
							public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
								Files.delete(file);
								return FileVisitResult.CONTINUE;
							}

							@Override
							public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
								Files.delete(dir);
								return FileVisitResult.CONTINUE;
							}
						});
					} catch (IOException e) {
						General.error("Error deleting " + configs);
					}
				}
			});
		}
		return configs.toString();
	}

	public static InetSocketAddress getAddressFromConfig(ConfigFactory config, String section, String option) {
		String addressAndPort = config.config.get("onion", "api_address", String.class);
		int colonPos = addressAndPort.lastIndexOf(':');
		return new InetSocketAddress(addressAndPort.substring(0, colonPos),
				(short) Integer.parseInt(addressAndPort.substring(colonPos + 1)));
	}

	public static Socket connectToAPI(RedirectBackupThread rbt, ConfigFactory config)
			throws IOException, InterruptedException {
		final Socket api = new Socket();
		while (!rbt.contains("Waiting for API connection on ")) {
			Thread.sleep(500);
		}
		api.connect(Helper.getAddressFromConfig(config, "onion", "api_address"));
		while (!rbt.contains("API connection successful")) {
			Thread.sleep(500);
		}
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					api.close();
				} catch (IOException e) {
					Helper.error(e.getMessage());
				}
			}
		});
		return api;
	}

	public static Socket connectToOnion(RedirectBackupThread rbt, ConfigFactory config)
			throws IOException, InterruptedException {
		final Socket onion = new Socket();
		while (!rbt.contains("Waiting for Onion connections on ")) {
			Thread.sleep(500);
		}
		onion.connect(
				new InetSocketAddress("127.0.0.1", config.config.get("onion", "p2p_port", Integer.class).intValue()));
		while (!rbt.contains("Onion connection successful")) {
			Thread.sleep(500);
		}
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					onion.close();
				} catch (IOException e) {
					Helper.error(e.getMessage());
				}
			}
		});
		return onion;
	}

	public static void info(String s) {
		System.out.println("TEST: INFO: " + s);
	}

	public static void warning(String s) {
		System.err.println("TEST: WARNING: " + s);
	}

	public static void error(String s) {
		System.err.println("TEST: ERROR: " + s);
	}

	public static void fatal(String s) {
		System.err.println("TEST: FATAL: " + s);
		System.exit(1);
	}

	public static class TestPeer extends Thread {
		private final ByteBuffer buffer;
		private Socket controlOnion;
		private final int size;
		protected final RedirectBackupThread rbt;

		public TestPeer(RedirectBackupThread rbt, ConfigFactory config) {
			this.rbt = rbt;
			size = config.config.get("onion", "p2p_packetsize", Integer.class);
			buffer = ByteBuffer.allocate(size + OnionMessage.ONION_HEADER_SIZE);
			try {
				controlOnion = Helper.connectToOnion(rbt, config);
			} catch (IOException | InterruptedException e) {
				Helper.fatal(e.getMessage());
			}
		}

		protected void writeControl(short id, byte data[]) throws IOException {
			OnionMessage message = new OnionMessage(size, id, null, data);
			message.serialize(buffer);
			controlOnion.getOutputStream().write(buffer.array());
		}

		protected byte[] readControl(short id) throws IOException {
			controlOnion.getInputStream().read(buffer.array());
			OnionMessage message = OnionMessage.parse(size, buffer, null);
			if (id == message.getId()) {
				return null;
			} else {
				return message.getData();
			}
		}
	}

	public static class RedirectBackupThread extends Thread {
		private final BufferedReader reader;
		private final StringBuilder backup = new StringBuilder(4096);

		public RedirectBackupThread(BufferedReader reader) {
			this.reader = reader;
		}

		@Override
		public void run() {
			try {
				for (;;) {
					String line = reader.readLine();
					if (line == null) {
						return;
					}
					System.out.println(line);
					backup.append(line);
				}
			} catch (IOException e) {
				return;
			}
		}

		public boolean contains(String match) {
			return backup.toString().contains(match);
		}
	}
}
