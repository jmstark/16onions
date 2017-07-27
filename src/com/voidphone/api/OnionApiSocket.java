package com.voidphone.api;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Random;

import com.voidphone.general.General;
import com.voidphone.general.SizeLimitExceededException;
import com.voidphone.general.Util;
import com.voidphone.onion.Main;
import com.voidphone.onion.OnionConnectingSocket;

import onion.api.OnionCoverMessage;
import onion.api.OnionErrorMessage;
import onion.api.OnionTunnelBuildMessage;
import onion.api.OnionTunnelDataMessage;
import onion.api.OnionTunnelDestroyMessage;
import onion.api.OnionTunnelIncomingMessage;
import onion.api.OnionTunnelReadyMessage;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;
import protocol.ProtocolException;
import protocol.Protocol.MessageType;

public class OnionApiSocket extends ApiSocket {
	protected Config config;
	protected OnionConnectingSocket currentTunnel;
	protected OnionConnectingSocket nextTunnel;
	protected InetSocketAddress tunnelDestination;
	protected byte[] destinationHostkey;
	public static final int ipAddressLength = 4;

	private final HashMap<Integer, Void> map;
	private final Random random;

	/**
	 * Listens for a new Onion API connection.
	 * 
	 * @param port
	 *            the port to listen on
	 * @throws IOException
	 *             if there is an I/O-error
	 */
	public OnionApiSocket(int port) throws IOException {
		super(port);
		random = new Random();
		map = new HashMap<Integer, Void>();
	}

	/**
	 * This function should be called shortly before a new round begins. It builds a
	 * second backup tunnel with the same end destination and external ID.
	 * 
	 * @throws Exception
	 */
	public void prepareNextTunnel() throws Exception {
		nextTunnel = new OnionConnectingSocket(tunnelDestination, destinationHostkey, config, currentTunnel.externalID);
	}

	/**
	 * This function should be called at the beginning of a new round. We switch
	 * over to the new tunnel and destroy the old one.
	 * 
	 * @throws Exception
	 */
	public void switchToNextTunnel() throws Exception {
		OnionConnectingSocket oldTunnel = currentTunnel;
		currentTunnel = nextTunnel;
		// register
		currentTunnel.registerChannel(Main.getSelector());
		oldTunnel.destroy();
		nextTunnel = null;
	}

	/**
	 * Returns a OnionTunnelReadyMessage.
	 * 
	 * @param id
	 *            the ID of the connection
	 * @param hostkey
	 *            the Hostkey
	 * @return the OnionTunnelReadyMessage
	 */
	public OnionTunnelReadyMessage newOnionTunnelReadyMessage(int id, byte hostkey[]) {
		if (!map.containsKey(id)) {
			throw new IllegalArgumentException("Illegal ID!");
		}
		try {
			return new OnionTunnelReadyMessage((long) id, hostkey);
		} catch (MessageSizeExceededException e) {
			General.fatalException(e);
			return null;
		}
	}

	/**
	 * Returns a OnionTunnelIncomingMessage.
	 * 
	 * @param id
	 *            the ID of the connection
	 * @param hostkey
	 *            the Hostkey
	 * @return the OnionTunnelIncomingMessage
	 */
	public OnionTunnelIncomingMessage newOnionTunnelIncomingMessage(int id, byte hostkey[]) {
		if (!map.containsKey(id)) {
			throw new IllegalArgumentException("Illegal ID!");
		}
		try {
			return new OnionTunnelIncomingMessage((long) id, hostkey);
		} catch (MessageSizeExceededException e) {
			General.fatalException(e);
			return null;
		}
	}

	/**
	 * Returns a OnionTunnelDataMessage.
	 * 
	 * @param id
	 *            the ID of the connection
	 * @param data
	 *            the data
	 * @return the OnionTunnelDataMessage
	 */
	public OnionTunnelDataMessage newOnionTunnelDataMessage(int id, byte data[]) {
		if (!map.containsKey(id)) {
			throw new IllegalArgumentException("Illegal ID!");
		}
		try {
			return new OnionTunnelDataMessage((long) id, data);
		} catch (MessageSizeExceededException e) {
			General.fatalException(e);
			return null;
		}
	}

	/**
	 * Returns a OnionErrorMessage.
	 * 
	 * @param id
	 *            the ID of the connection
	 * @param requestType
	 *            the requestType
	 * @return the OnionErrorMessage
	 */
	public OnionErrorMessage newOnionErrorMessage(int id, Protocol.MessageType requestType) {
		if (!map.containsKey(id)) {
			throw new IllegalArgumentException("Illegal ID!");
		}
		return new OnionErrorMessage(requestType, (long) id);
	}

	/**
	 * Sent from CM/UI to Onion to build an onion tunnel.
	 * 
	 * @param otbm
	 *            the OnionTunnelBuildMessage
	 * @param connection
	 *            the connection from which the packet was received
	 */
	private void ONIONTUNNELBUILD(OnionTunnelBuildMessage otbm) {
		try {
			// build the tunnel
			tunnelDestination = otbm.getAddress();
			RSAPublicKey key = otbm.getKey();
			currentTunnel = new OnionConnectingSocket(tunnelDestination, Util.getHostkeyBytes(otbm.getKey()), config);

			// register
			currentTunnel.registerChannel(Main.getSelector());

			// reply
			ONIONTUNNELREADY(newOnionTunnelReadyMessage(currentTunnel.externalID, Util.getHostkeyBytes(key)));
		} catch (Exception e) {
			e.printStackTrace();
		}
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
	public void ONIONTUNNELREADY(OnionTunnelReadyMessage otrm) {
		connection.sendMsg(otrm);
	}

	/**
	 * Sent from CM/UI to Onion to destroy an onion tunnel.
	 * 
	 * @param otdm
	 *            the OnionTunnelDestroyMessage
	 * @param connection
	 *            the connection from which the packet was received
	 */
	private void ONIONTUNNELDESTROY(OnionTunnelDestroyMessage otdm) {

		int tunnelId = (int) otdm.getId();

		// destroy main and backup tunnel
		if (currentTunnel.externalID == tunnelId) {
			try {
				currentTunnel.destroy();
				nextTunnel.destroy();
				tunnelDestination = null;
				currentTunnel = null;
				nextTunnel = null;
			} catch (Exception e) {
				// This could be normal, see TODO
				e.printStackTrace();
			}
		}
		// TODO: check for destruction request of OnionListenerSocket
	}

	/**
	 * Sent from Onion to CM/UI to signal an incoming onion tunnel.
	 * 
	 * @param otim
	 *            the OnionTunnelIncomingMessage
	 * @param connection
	 *            the connection to which the packet will be sent
	 */
	public void ONIONTUNNELINCOMING(OnionTunnelIncomingMessage otim) {
		connection.sendMsg(otim);
	}

	/**
	 * Sent from CM/UI to Onion to send data through the tunnel.
	 * 
	 * @param otdm
	 *            the OnionTunnelDataMessage
	 * @param connection
	 *            the connection from which the packet was received
	 */
	private void ONIONTUNNELDATAOUTGOING(OnionTunnelDataMessage otdm) {
		try {
			currentTunnel.sendRealData(otdm.getData());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Sent from Onion to CM/UI to signal an incoming packet from the onion tunnel.
	 * 
	 * @param otdm
	 *            the OnionTunnelDataMessage
	 * @param connection
	 *            the connection to which the packet will be sent
	 */
	public void ONIONTUNNELDATAINCOMING(OnionTunnelDataMessage otdm) {
		connection.sendMsg(otdm);
	}

	/**
	 * Sent from Onion to CM/UI to signal an error caused by an earlier request.
	 * 
	 * @param oem
	 *            the OnionErrorMessage
	 * @param connection
	 *            the connection to which the packet will be sent
	 */
	public void ONIONERROR(OnionErrorMessage oem) {
		connection.sendMsg(oem);
	}

	/**
	 * Sent from CM/UI to Onion to send cover data through the tunnel.
	 * 
	 * @param ocm
	 *            the OnionCoverMessage
	 * @param connection
	 *            the connection from which the packet was received
	 */
	private void ONIONCOVER(OnionCoverMessage ocm) {
		try {
			currentTunnel.sendCoverData(ocm.getSize());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Parses the received message and calls the proper method.
	 * 
	 * @param buffer
	 *            the received message
	 * @param type
	 *            the type of the received message
	 * @throws MessageParserException
	 *             if there is an error while parsing the message
	 * @throws ProtocolException
	 *             if the message does not match the protocol
	 */
	@Override
	protected void receive(ByteBuffer buffer, MessageType type) throws MessageParserException, ProtocolException {
		switch (type) {
		case API_ONION_COVER:
			ONIONCOVER(OnionCoverMessage.parse(buffer));
			return;
		case API_ONION_TUNNEL_BUILD:
			ONIONTUNNELBUILD(OnionTunnelBuildMessage.parse(buffer));
			return;
		case API_ONION_TUNNEL_DATA:
			ONIONTUNNELDATAOUTGOING(OnionTunnelDataMessage.parse(buffer));
			return;
		case API_ONION_TUNNEL_DESTROY:
			ONIONTUNNELDESTROY(OnionTunnelDestroyMessage.parse(buffer));
			return;
		default:
			throw new ProtocolException("Unexpected message received");
		}

	}

	/**
	 * Registers a new logical connection to the CM/UI module.
	 * 
	 * @return ID of the new connection
	 * @throws SizeLimitExceededException
	 *             if too many connections are registered
	 */
	@Override
	public int register() throws SizeLimitExceededException {
		int id;

		if (map.size() >= Integer.MAX_VALUE) {
			throw new SizeLimitExceededException("Too many connections registered!");
		}
		do {
			id = random.nextInt();
		} while (map.containsKey(id));
		map.put(id, null);
		return id;
	}

	/**
	 * Unregisters a logical connection.
	 * 
	 * @param id
	 *            ID of the connection
	 * @throws IllegalArgumentException
	 *             if the ID was not registered
	 */
	@Override
	public void unregister(int id) throws IllegalArgumentException {
		if (!map.containsKey(id)) {
			throw new IllegalArgumentException("Illegal ID!");
		}
		map.remove(id);
	}
}
