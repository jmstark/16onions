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
package com.voidphone.onion;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Random;
import com.voidphone.api.Config;
import com.voidphone.api.OnionApiSocket;
import com.voidphone.api.OnionAuthApiSocket;
import com.voidphone.api.OnionPeer;
import com.voidphone.general.Util;
import auth.api.OnionAuthSessionHS1;
import auth.api.OnionAuthSessionIncomingHS2;
import auth.api.OnionAuthSessionStartMessage;
import onion.api.OnionTunnelIncomingMessage;


/**
 * When the main application wants to build a tunnel to some node, it creates an
 * instance of this class, passing destination address and hostkey and the
 * hopcount to the constructor.
 */
public class OnionConnectingSocket extends OnionBaseSocket {

	protected InetSocketAddress destAddr;
	protected byte[] destHostkey;
	protected InetSocketAddress nextHopAddress;
	protected int rpsApiId;
	protected int authApiId;
	protected short nextHopMId;


	/**
	 * Constructor
	 * 
	 * @param destAddr
	 *            the address of the target node (i.e. the last hop)
	 * @param destHostkey
	 *            the hostkey of the target node
	 * @param config
	 *            configuration
	 * @param hopCount
	 *            the number of intermediate hops (excluding our node and the target
	 *            node)
	 * @param externalID
	 *            Other modules use this ID to refer to this tunnel (backup tunnels
	 *            with the same end destination must get the same ID, therefore this
	 *            input parameter) If building a backup tunnel, use this constructor
	 *            with the same ID as the existing main tunnel, otherwise use the
	 *            second constructor which assigns a new ID.
	 * @throws Exception
	 */
	public OnionConnectingSocket(Multiplexer m, InetSocketAddress destAddr, byte[] destHostkey, 
			Config config, int hopCount, int externalID) throws Exception {
		super(m, externalID);
		this.config = config;
		this.destAddr = destAddr;
		this.destHostkey = destHostkey;
		authSessionIds = new int[hopCount + 1];
		rpsApiId = Main.getRas().register();
		authApiId = Main.getOaas().register();

		// Fill up an array with intermediate hops and the target node
		OnionPeer[] hops = new OnionPeer[hopCount + 1];
		for (int i = 0; i < hopCount; i++) {
			hops[i] = new OnionPeer(Main.getRas().RPSQUERY(Main.getRas().newRpsQueryMessage(rpsApiId)));
		}
		// If end node is unspecified, use another random node
		if (destAddr == null || destHostkey == null)
			hops[hopCount] = new OnionPeer(Main.getRas().RPSQUERY(Main.getRas().newRpsQueryMessage(rpsApiId)));
		else
			hops[hopCount] = new OnionPeer(destAddr, destHostkey);

		// Connect to first hop - all other connections are forwarded over this hop
		m.registerAddress(hops[0].address);
		nextHopMId = m.registerID(hops[0].address);
		
		// Bind UDP socket to the same local port as the TCP socket so so we can correlate
		authSessionIds[0] = authenticate(hops[0].hostkey, 0);

		// Establish forwardings (if any)
		for (int i = 1; i < hops.length; i++) {
			// Send forwarding request to node i-1 ->
			// it connects to the next node i and then forwards there
			// everything it receives from that point on.

			// First, send the actual request along with the target address. Encrypted.
			ByteArrayOutputStream outgoingDataBAOS = new ByteArrayOutputStream();
			DataOutputStream outgoingData = new DataOutputStream(outgoingDataBAOS);
			
			outgoingData.writeShort(MSG_BUILD_TUNNEL);
			byte[] rawAddress = hops[i].address.getAddress().getAddress();
			outgoingData.write((byte) rawAddress.length);
			outgoingData.write(rawAddress);
			outgoingData.writeInt(hops[i].address.getPort());
			byte[] encryptedPayload = encrypt(outgoingDataBAOS.toByteArray(), i);

			m.write(new OnionMessage(nextHopMId, OnionMessage.CONTROL_MESSAGE, nextHopAddress, encryptedPayload));
			// Now, we are indirectly connected to the target node. 
			// Authenticate to that node.
			authSessionIds[i] = authenticate(hops[i].hostkey, i);

		}

		nextHopAddress = hops[0].address;
	}
	
	
	/**
	 * 
	 * 
	 * @param destAddr
	 *            the address of the target node (i.e. the last hop)
	 * @param destHostkey
	 *            the hostkey of the target node
	 * @param config
	 *            configuration
	 * @param hopCount
	 *            the number of intermediate hops (excluding our node and the target
	 *            node)
	 * @throws Exception
	 */
	public OnionConnectingSocket(Multiplexer m, InetSocketAddress destAddr, byte[] destHostkey, Config config) throws Exception {
		this(m, destAddr, destHostkey, config, config.hopCount, idCounter++);
	}


	/**
	 * 
	 * 
	 * @param destAddr
	 *            the address of the target node (i.e. the last hop)
	 * @param destHostkey
	 *            the hostkey of the target node
	 * @param config
	 *            configuration
	 * @param hopCount
	 *            the number of intermediate hops (excluding our node and the target
	 *            node)
	 * @throws Exception
	 */
	public OnionConnectingSocket(Multiplexer m, InetSocketAddress destAddr, byte[] destHostkey, Config config, int externalID)
			throws Exception {
		this(m, destAddr, destHostkey, config, config.hopCount, externalID);

	}
	
	
	/**
	 * Encrypts payload for end hop.
	 * 
	 * @return encrypted payload
	 * @throws Exception
	 */
	protected byte[] encrypt(byte[] payload) throws Exception {
		return super.encrypt(payload, authSessionIds.length);
	}

	/**
	 * Decrypts payload of end hop.
	 * 
	 * @return Decrypted payload
	 * @throws Exception
	 */
	protected byte[] decrypt(byte[] payload) throws Exception {
		return super.decrypt(payload, authSessionIds.length);
	}
	


	/**
	 * Authenticates via OnionAuth. Encrypts with numLayers (0 = no encryption).
	 * 
	 * @param hopHostkey
	 * @param numLayers
	 * @return sessionID
	 * @throws Exception
	 */
	public int authenticate(byte[] hopHostkey, int numLayers) throws Exception {

		ByteArrayOutputStream outgoingDataBAOS = new ByteArrayOutputStream();
		DataOutputStream outGoingData = new DataOutputStream(outgoingDataBAOS);
		OnionAuthSessionHS1 hs1;

		outGoingData.writeInt(MAGIC_SEQ_CONNECTION_START);
		outGoingData.writeInt(VERSION);

		// get hs1 from onionAuth and send it to remote peer
		hs1 = Main.getOaas().AUTHSESSIONSTART(
				Main.getOaas().newOnionAuthSessionStartMessage(apiRequestCounter++, Util.getHostkeyObject(hopHostkey)));
		outGoingData.writeInt(hs1.getPayload().length);
		outGoingData.write(hs1.getPayload());


		byte[] encryptedPayload = encrypt(outgoingDataBAOS.toByteArray(), numLayers);
		m.write(new OnionMessage(nextHopMId,OnionMessage.CONTROL_MESSAGE, nextHopAddress,encryptedPayload));

		// read incoming hs2 into buffer. We need to know the length of hs2
		OnionMessage incomingMessage = m.read(nextHopMId, nextHopAddress);
		byte[] hs2payload = decrypt(incomingMessage.data, numLayers);
		
		Main.getOaas().AUTHSESSIONINCOMINGHS2(
				new OnionAuthSessionIncomingHS2(hs1.getSessionID(), apiRequestCounter++, hs2payload));

		return hs1.getSessionID();
	}

	
	/**
	 * Sends data through the tunnel, be it real VOIP or cover traffic.
	 * @param isRealData
	 * @param data
	 * @throws Exception 
	 */
	public void sendData(boolean isRealData, byte[] data) throws Exception
	{
		byte[] payload = new byte[data.length + 1];
		payload[0] = isRealData ? MSG_DATA : MSG_COVER;
		
		m.write(new OnionMessage(nextHopMId, OnionMessage.CONTROL_MESSAGE, nextHopAddress, encrypt(payload)));		
	}
	
	public void sendRealData(byte[] data) throws Exception
	{
		sendData(true,data);
	}
	
	public void sendCoverData(int size) throws Exception
	{
		byte[] rndData = new byte[size];
		new Random().nextBytes(rndData);
		sendData(false, rndData);
	}


	public void destroy() throws Exception {
		byte[] plainMsg = {MSG_DESTROY_TUNNEL};
		// send the message iteratively to all hops,
		// starting at the farthest one
		for (int i = authSessionIds.length; i > 0; i--) {
			byte[] encryptedMsg = encrypt(plainMsg, i);
			m.write(new OnionMessage(nextHopMId, OnionMessage.CONTROL_MESSAGE, nextHopAddress, encryptedMsg));
		}
		//Close connection
		m.unregisterID(nextHopMId, nextHopAddress);
		//Close auth and rps API connections
		Main.getOaas().unregister(authApiId);
		Main.getRas().unregister(rpsApiId);
	}


	public void getAndProcessNextDataMessage() throws Exception {
		
		OnionMessage incomingMessage = m.read(nextHopMId, nextHopAddress);
		// decrypt data
		byte[] payload = decrypt(incomingMessage.data);
		
		if(payload[0] != MSG_DATA)
			//ignore cover traffic
			return;
		
		Main.getOas().ONIONTUNNELINCOMING(Main.getOas().newOnionTunnelIncomingMessage(externalID, payload));
			
		return;
	}


}
