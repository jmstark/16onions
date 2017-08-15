package com.voidphone.testing.tests.onion;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.interfaces.RSAPublicKey;

import com.voidphone.testing.Helper;

import onion.api.OnionErrorMessage;
import onion.api.OnionTunnelBuildMessage;
import onion.api.OnionTunnelDataMessage;
import onion.api.OnionTunnelIncomingMessage;
import onion.api.OnionTunnelReadyMessage;
import protocol.Connection;
import protocol.MessageHandler;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;
import protocol.ProtocolException;

public abstract class Context extends MessageHandler<Void> {
	private final Connection connection;
	protected long id;
	protected boolean tunnelPresent;
	private boolean killed;

	public Context(Connection connection, RSAPublicKey targetHostkey, InetSocketAddress targetAddress) {
		this(connection);
		OnionTunnelBuildMessage onionTunnelBuildMessage;
		try {
			onionTunnelBuildMessage = new OnionTunnelBuildMessage(targetAddress, targetHostkey);
		} catch (MessageSizeExceededException ex) {
			throw new RuntimeException("Message size exceeded");
		}
		connection.sendMsg(onionTunnelBuildMessage);
	}

	public Context(Connection connection) {
		super(null);
		this.connection = connection;
		tunnelPresent = false;
		killed = false;
		connection.receive(this);
	}

	/**
	 * Send data on the tunnel with the given ID
	 *
	 * @param id
	 *            the tunnel ID to send the data on
	 */
	protected void sendData(long id, String actual) {
		OnionTunnelDataMessage message;
		try {
			message = new OnionTunnelDataMessage(id, actual.getBytes());
		} catch (MessageSizeExceededException ex) {
			throw new RuntimeException();
		}
		connection.sendMsg(message);
	}

	protected abstract void ONIONTUNNELREADY(OnionTunnelReadyMessage msg) throws ProtocolException;

	protected abstract void ONIONTUNNELINCOMING(OnionTunnelIncomingMessage msg) throws ProtocolException;

	protected abstract void ONIONTUNNELDATA(OnionTunnelDataMessage msg) throws ProtocolException;

	@Override
	public void parseMessage(ByteBuffer buf, Protocol.MessageType type, Void closure)
			throws MessageParserException, ProtocolException {
		if (killed) {
			Helper.error("Tunnel was killed!");
			return;
		}
		switch (type) {
		case API_ONION_TUNNEL_READY: {
			if (tunnelPresent) {
				throw new ProtocolException("Received Onion Tunnel Ready, but we do not expect any other tunnel!");
			}
			ONIONTUNNELREADY(OnionTunnelReadyMessage.parse(buf));
			return;
		}
		case API_ONION_TUNNEL_INCOMING: {
			if (tunnelPresent) {
				throw new ProtocolException("Received Onion Tunnel Incoming, but we do not expect any other tunnel!");
			}
			ONIONTUNNELINCOMING(OnionTunnelIncomingMessage.parser(buf));
			return;
		}
		case API_ONION_TUNNEL_DATA: {
			if (!tunnelPresent) {
				throw new ProtocolException("Cannot receive data without a tunnel!");
			}
			OnionTunnelDataMessage msg = OnionTunnelDataMessage.parse(buf);
			if (id != msg.getId()) {
				throw new ProtocolException("Got message with unknown ID!");
			}
			Helper.info("Got correct data packet!");
			ONIONTUNNELDATA(msg);
			return;
		}
		case API_ONION_ERROR: {
			OnionErrorMessage msg = OnionErrorMessage.parser(buf);
			Helper.error("Received Onion error with ID " + msg.getId() + "!");
			killed = true;
			return;
		}
		default:
			throw new ProtocolException("Received unknown message type " + type.name());
		}
	}
}