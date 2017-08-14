package com.voidphone.testing;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.security.InvalidKeyException;
import java.security.interfaces.RSAPublicKey;
import java.util.Random;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import onion.OnionConfiguration;
import onion.OnionConfigurationImpl;
import onion.api.OnionTunnelDataMessage;
import onion.api.OnionTunnelIncomingMessage;
import onion.api.OnionTunnelReadyMessage;
import protocol.Connection;
import protocol.DisconnectHandler;
import protocol.ProtocolException;
import util.PEMParser;
import util.Program;
import util.config.CliParser;

public class OnionTest extends Program {
	private InetSocketAddress api_address;
	private InetSocketAddress target_api_address;
	private InetSocketAddress targetAddress;
	private Connection connection1;
	private Connection connection2;
	private RSAPublicKey targetHostkey;
	private long number;

	public OnionTest() {
		super("com.voidphone.testing", "API conformance test case for ONION");
		number = new Random().nextLong();
	}

	@Override
	protected void addParserOptions(CliParser parser) {
		// Add options for the target hostkey and target onion P2P address
		parser.addOption(Option.builder("d").required(true).longOpt("second config").desc("Target config")
				.optionalArg(false).argName("FILE").hasArg().build());
		parser.addOption(Option.builder("k").required(true).longOpt("hostkey").desc("Target hostkey").optionalArg(false)
				.argName("FILE").hasArg().build());
		parser.addOption(Option.builder("t").required(true).longOpt("address").desc("Target ONION P2P address")
				.optionalArg(false).argName("IP ADDRESS").hasArg().build());
		parser.addOption(Option.builder("p").required(true).longOpt("address").desc("Target ONION P2P address port")
				.optionalArg(false).argName("PORT").hasArg().build());
	}

	@Override
	protected void parseCommandLine(CommandLine cli, CliParser parser) {
		String filename = parser.getConfigFilename("onion.conf");
		OnionConfiguration config;
		try {
			config = new OnionConfigurationImpl(filename);
		} catch (IOException ex) {
			throw new RuntimeException("Unable to read config file");
		}
		String second_config = cli.getOptionValue("d");
		try {
			OnionConfigurationImpl secondConfig = new OnionConfigurationImpl(second_config);
			target_api_address = secondConfig.getAPIAddress();
		} catch (IOException ex) {
			throw new RuntimeException("Unable to read config file");
		}
		api_address = config.getAPIAddress();
		// retrieve the target hostkey
		String target_hostkey = cli.getOptionValue("k");
		String hostname = cli.getOptionValue("t");
		String port = cli.getOptionValue("p");
		targetAddress = new InetSocketAddress(hostname, Integer.parseInt(port));
		try {
			targetHostkey = PEMParser.getPublicKeyFromPEM(new File(target_hostkey));
		} catch (IOException | InvalidKeyException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	protected void cleanup() {
		if (null != connection1) {
			connection1.disconnect();
			connection2 = null;
		}
		if (null != connection2) {
			connection2.disconnect();
			connection2 = null;
		}
	}

	@Override
	protected void run() {
		AsynchronousSocketChannel channel1;
		AsynchronousSocketChannel channel2;
		try {
			channel1 = AsynchronousSocketChannel.open(this.group);
			channel2 = AsynchronousSocketChannel.open(this.group);
		} catch (IOException ex) {
			throw new RuntimeException();
		}
		channel2.connect(target_api_address, channel2, new CompletionHandler<Void, AsynchronousSocketChannel>() {
			@Override
			public void completed(Void none, AsynchronousSocketChannel channel) {
				connection2 = new Connection(channel, new DisconnectHandler<Void>(null) {
					@Override
					protected void handleDisconnect(Void closure) {
						if (!OnionTest.this.inShutdown()) {
							Helper.fatal("Onion API disconnected!");
							connection2 = null;
							shutdown();
						}
					}
				});
				new Context2(number, connection2);
			}

			@Override
			public void failed(Throwable t, AsynchronousSocketChannel asc) {
				Helper.fatal("Cannot connect to Onion API!");
				shutdown();
			}
		});
		channel1.connect(api_address, channel1, new CompletionHandler<Void, AsynchronousSocketChannel>() {
			@Override
			public void completed(Void none, AsynchronousSocketChannel channel) {
				connection1 = new Connection(channel, new DisconnectHandler<Void>(null) {
					@Override
					protected void handleDisconnect(Void closure) {
						if (!OnionTest.this.inShutdown()) {
							Helper.fatal("Onion API disconnected!");
							connection1 = null;
							shutdown();
						}
					}
				});
				new Context1(number, connection1, targetHostkey, targetAddress);
			}

			@Override
			public void failed(Throwable t, AsynchronousSocketChannel asc) {
				Helper.fatal("Cannot connect to Onion API!");
				shutdown();
			}
		});
	}

	public static void main(String args[]) throws IOException {
		new OnionTest().start(args);
	}

	private static class Context1 extends Context {
		private long number;

		public Context1(long number, Connection connection, RSAPublicKey targetHostkey,
				InetSocketAddress targetAddress) {
			super(connection, targetHostkey, targetAddress);
			this.number = number;
		}

		@Override
		protected void ONIONTUNNELREADY(OnionTunnelReadyMessage msg) throws ProtocolException {
			super.id = msg.getId();
			super.tunnelPresent = true;
			super.sendData(super.id, number++ + "");
		}

		@Override
		protected void ONIONTUNNELINCOMING(OnionTunnelIncomingMessage msg) throws ProtocolException {
			throw new ProtocolException("We do not expect any other tunnel!");
		}

		@Override
		protected void ONIONTUNNELDATA(OnionTunnelDataMessage msg) throws ProtocolException {
			long n = Long.parseLong(new String(msg.getData()));
			if (n != number) {
				Helper.fatal("Received wrong number: Expected " + number + ", got " + n + "!");
			}
			super.sendData(super.id, number++ + "");
		}
	}

	private static class Context2 extends Context {
		private long number;

		public Context2(long number, Connection connection) {
			super(connection);
			this.number = number;
		}

		@Override
		protected void ONIONTUNNELREADY(OnionTunnelReadyMessage msg) throws ProtocolException {
			throw new ProtocolException("We do not expect any other tunnel!");
		}

		@Override
		protected void ONIONTUNNELINCOMING(OnionTunnelIncomingMessage msg) throws ProtocolException {
			super.id = msg.getTunnelID();
			super.tunnelPresent = true;
		}

		@Override
		protected void ONIONTUNNELDATA(OnionTunnelDataMessage msg) throws ProtocolException {
			long n = Long.parseLong(new String(msg.getData()));
			if (n != number) {
				Helper.fatal("Received wrong number: Expected " + number + ", got " + n + "!");
			}
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}
			super.sendData(super.id, ++number + "");
		}
	}
}
