package com.voidphone.api;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;

import com.voidphone.general.General;

import onion.api.OnionCoverMessage;
import onion.api.OnionErrorMessage;
import onion.api.OnionTunnelBuildMessage;
import onion.api.OnionTunnelDataMessage;
import onion.api.OnionTunnelDestroyMessage;
import onion.api.OnionTunnelIncomingMessage;
import onion.api.OnionTunnelReadyMessage;
import protocol.Connection;
import protocol.MessageHandler;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;
import protocol.ProtocolException;
import protocol.ProtocolServer;
import protocol.Protocol.MessageType;

public class OnionApiSocket2 extends ProtocolServer<Connection> {
	protected OnionApiSocket2(SocketAddress socketAddress, AsynchronousChannelGroup channelGroup) throws IOException {
		super(socketAddress, channelGroup);
	}

	@Override
	protected Connection handleNewClient(Connection connection) {
		connection.receive(new OnionMessageHandler(connection));
		return connection;
	}

	@Override
	protected void handleDisconnect(Connection closure) {

	}

	public OnionTunnelReadyMessage newOnionTunnelReadyMessage(long tunnelID, byte hostkey[]) {
		try {
			return new OnionTunnelReadyMessage(tunnelID, hostkey);
		} catch (MessageSizeExceededException e) {
			General.fatalException(e);
			return null;
		}
	}

	public OnionTunnelIncomingMessage newOnionTunnelIncomingMessage(long tunnelID, byte hostkey[]) {
		try {
			return new OnionTunnelIncomingMessage(tunnelID, hostkey);
		} catch (MessageSizeExceededException e) {
			General.fatalException(e);
			return null;
		}
	}

	public OnionTunnelDataMessage newOnionTunnelDataMessage(long tunnelID, byte data[]) {
		try {
			return new OnionTunnelDataMessage(tunnelID, data);
		} catch (MessageSizeExceededException e) {
			General.fatalException(e);
			return null;
		}
	}

	public OnionErrorMessage newOnionErrorMessage(Protocol.MessageType requestType, long tunnelID) {
		return new OnionErrorMessage(requestType, tunnelID);
	}

	/**
	 * Sent from CM/UI to Onion to build an onion tunnel.
	 * 
	 * @param otbm
	 *            the OnionTunnelBuildMessage
	 * @param connection
	 *            the connection from which the packet was received
	 */
	private void ONIONTUNNELBUILD(OnionTunnelBuildMessage otbm, Connection connection) {

	}

	/**
	 * Sent from Onion to CM/UI to confirm that the previously requested tunnel is
	 * ready.
	 * 
	 * @param otrm
	 *            the OnionTunnelReadyMessage
	 * @param connection
	 *            the connection to which the packet will be sent
	 */
	public void ONIONTUNNELREADY(OnionTunnelReadyMessage otrm, Connection connection) {

	}

	/**
	 * Sent from CM/UI to Onion to destroy an onion tunnel.
	 * 
	 * @param otdm
	 *            the OnionTunnelDestroyMessage
	 * @param connection
	 *            the connection from which the packet was received
	 */
	private void ONIONTUNNELDESTROY(OnionTunnelDestroyMessage otdm, Connection connection) {

	}

	/**
	 * Sent from Onion to CM/UI to signal an incoming onion tunnel.
	 * 
	 * @param otim
	 *            the OnionTunnelIncomingMessage
	 * @param connection
	 *            the connection to which the packet will be sent
	 */
	public void ONIONTUNNELINCOMING(OnionTunnelIncomingMessage otim, Connection connection) {

	}

	/**
	 * Sent from CM/UI to Onion to send data through the tunnel.
	 * 
	 * @param otdm
	 *            the OnionTunnelDataMessage
	 * @param connection
	 *            the connection from which the packet was received
	 */
	private void ONIONTUNNELDATAOUTGOING(OnionTunnelDataMessage otdm, Connection connection) {

	}

	/**
	 * Sent from Onion to CM/UI to signal an incoming packet from the onion tunnel.
	 * 
	 * @param otdm
	 *            the OnionTunnelDataMessage
	 * @param connection
	 *            the connection to which the packet will be sent
	 */
	public void ONIONTUNNELDATAINCOMING(OnionTunnelDataMessage otdm, Connection connection) {

	}

	/**
	 * Sent from Onion to CM/UI to signal an error caused by an earlier request.
	 * 
	 * @param oem
	 *            the OnionErrorMessage
	 * @param connection
	 *            the connection to which the packet will be sent
	 */
	public void ONIONERROR(OnionErrorMessage oem, Connection connection) {

	}

	/**
	 * Sent from CM/UI to Onion to send cover data through the tunnel.
	 * 
	 * @param ocm
	 *            the OnionCoverMessage
	 * @param connection
	 *            the connection from which the packet was received
	 */
	private void ONIONCOVER(OnionCoverMessage ocm, Connection connection) {

	}

	private class OnionMessageHandler extends MessageHandler<Connection> {
		private OnionMessageHandler(Connection closure) {
			super(closure);
		}

		@Override
		public void parseMessage(ByteBuffer buf, MessageType type, Connection closure)
				throws MessageParserException, ProtocolException {
			switch (type) {
			case API_ONION_COVER:
				ONIONCOVER(OnionCoverMessage.parse(buf), closure);
				return;
			case API_ONION_TUNNEL_BUILD:
				ONIONTUNNELBUILD(OnionTunnelBuildMessage.parse(buf), closure);
				return;
			case API_ONION_TUNNEL_DATA:
				ONIONTUNNELDATAOUTGOING(OnionTunnelDataMessage.parse(buf), closure);
				return;
			case API_ONION_TUNNEL_DESTROY:
				ONIONTUNNELDESTROY(OnionTunnelDestroyMessage.parse(buf), closure);
				return;
			default:
				throw new ProtocolException("Unexpected message received");
			}
		}
	}
}
