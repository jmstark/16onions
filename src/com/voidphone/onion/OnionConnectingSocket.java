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
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Random;
import com.voidphone.api.Config;
import com.voidphone.api.OnionPeer;
import com.voidphone.general.General;
import com.voidphone.general.NoRpsPeerException;
import com.voidphone.general.Util;

import auth.api.OnionAuthDecryptResp;
import auth.api.OnionAuthEncryptResp;
import auth.api.OnionAuthSessionHS1;
import rps.api.RpsPeerMessage;


/**
 * When the main application wants to build a tunnel to some node, it creates an
 * instance of this class.
 */
public class OnionConnectingSocket extends OnionBaseSocket {

	protected InetSocketAddress destAddr;
	protected byte[] destHostkey;
	protected InetSocketAddress nextHopAddress;
	protected int rpsApiId;
	protected short nextHopMId;


	/**
	 * Constructor
	 * 
	 * Construct a new OnionConnectingSocket to the specified target (destAddr and destHostkey) or
	 * a random node if destAddr or destHostkey are null.
	 * 
	 * @param m The multiplexer over which we communicate with other peers
	 * @param destAddr the address of the target node (i.e. the last hop)
	 * @param destHostkey the hostkey of the target node (i.e. the last hop)
	 * @param externalID
	 *            Other modules use this ID to refer to this tunnel (backup tunnels
	 *            with the same end destination must get the same ID, therefore this
	 *            input parameter). When building a backup tunnel, use this constructor
	 *            with the same ID as the existing main tunnel, otherwise use the
	 *            second constructor which assigns a new ID.
	 * @throws Exception
	 */
	public OnionConnectingSocket(Multiplexer m, InetSocketAddress destAddr, byte[] destHostkey, int externalID) throws Exception {
		super(m, externalID);
		int hopCount = Main.getConfig().hopCount;
		this.destAddr = destAddr;
		this.destHostkey = destHostkey;
		authSessionIds = new int[hopCount + 1];
		rpsApiId = Main.getRas().register();
		authApiId = Main.getOaas().register();

		// Fill up an array with intermediate hops and the target node
		OnionPeer[] hops = new OnionPeer[hopCount + 1];
		RpsPeerMessage rpsMsg;
		for (int i = 0; i < hopCount; i++) {
			rpsMsg = Main.getRas().RPSQUERY(Main.getRas().newRpsQueryMessage(rpsApiId));
			if(rpsMsg == null)
				throw new NoRpsPeerException();
			hops[i] = new OnionPeer(rpsMsg);
		}
		
		// If end node is unspecified, use another random node
		if (destAddr == null || destHostkey == null)
			hops[hopCount] = new OnionPeer(Main.getRas().RPSQUERY(Main.getRas().newRpsQueryMessage(rpsApiId)));
		else
			hops[hopCount] = new OnionPeer(destAddr, destHostkey);

		nextHopAddress = hops[0].address;
		
		// Connect to first hop - all other connections are forwarded over this hop
		m.registerAddress(hops[0].address);
		nextHopMId = m.registerID(hops[0].address);
		
		General.info("Connected to first hop");
		
		// Bind UDP socket to the same local port as the TCP socket so so we can correlate
		authSessionIds[0] = authenticate(hops[0].hostkey, 0);

		General.info("Authenticated to first hop");
		
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
			
			General.info("Sent tunnel building request to hop " + (i-1) );
			
			// Now, we are indirectly connected to the target node. 
			// Authenticate to that node.
			authSessionIds[i] = authenticate(hops[i].hostkey, i);
			
			General.info("authenticated to hop " + i);
			

		}
		
		//Signal to the last hop, that it is the target so it can signal an incoming tunnel to the CM
		ByteArrayOutputStream outgoingDataBAOS = new ByteArrayOutputStream();
		DataOutputStream outgoingData = new DataOutputStream(outgoingDataBAOS);
		outgoingData.writeShort(MSG_INCOMING_TUNNEL);
		outgoingData.writeInt(externalID);
		m.write(new OnionMessage(nextHopMId, OnionMessage.CONTROL_MESSAGE, nextHopAddress, encrypt(outgoingDataBAOS.toByteArray())));
		
		General.info("Signaled tunnel end to last hop");
		
		//Inform our CM, that the requested tunnel is ready.
		Main.getOas().ONIONTUNNELREADY(Main.getOas().newOnionTunnelReadyMessage(externalID, destHostkey));
		
		General.info("Signalled successfull tunnel build to CM");
	}
	
	
	/**
	 * Alternative constructor - a new externalID is assigned implicitely
	 * 
	 * @param m The multiplexer over which we communicate with other peers
	 * @param destAddr the address of the target node (i.e. the last hop)
	 * @param destHostkey the hostkey of the target node (i.e. the last hop)
	 * @throws Exception
	 */
	public OnionConnectingSocket(Multiplexer m, InetSocketAddress destAddr, byte[] destHostkey) throws Exception {
		this(m, destAddr, destHostkey, new Random().nextInt());
	}

	
	/**
	 * Encrypts payload for end hop.
	 * @param payload the plaintext payload
	 * @return encrypted payload
	 * @throws Exception
	 */
	protected byte[] encrypt(byte[] payload) throws Exception {
		return encrypt(payload, authSessionIds.length);
	}

	/**
	 * Decrypts payload of end hop.
	 * 
	 * @param payload the encrypted payload
	 * @return Decrypted payload
	 * @throws Exception
	 */
	protected byte[] decrypt(byte[] payload) throws Exception {
		return decrypt(payload, authSessionIds.length);
	}
	
	/**
	 * Encrypts payload with the given number of layers, 0 layers = no encryption.
	 * This method is only used during authentication phase, after that only
	 * encrypt(byte[]) is used.
	 * 
	 * @param payload the payload which will be encrypted
	 * @param numLayers number of encryption layers to apply. 0 = no encryption.
	 * @return encrypted payload (or plaintext if numLayers == 0)
	 * @throws Exception 
	 */
	protected byte[] encrypt(byte[] payload, int numLayers) throws Exception
	{
		if(numLayers < 0)
			throw new Exception("Negative number of layers");
		
		if(numLayers == 0)
		{
			//unencrypted packet
			return payload;
		}
			
		// Make an array containing the sessionIds in reverse order,
		// so that each hop can "peel off" one layer
		int[] neededSessionIds = new int[numLayers];
		for(int i=0; i < numLayers; i++)
		{
			neededSessionIds[i] = authSessionIds[numLayers-1-i];
		}
		
		//send the data to OnionAuth API and get encrypted data back.
		OnionAuthEncryptResp response = 
				Main.getOaas().AUTHLAYERENCRYPT(Main.getOaas().newOnionAuthEncrypt(authApiId, neededSessionIds, payload));
		
		General.info("");

		return response.getPayload();
	}
	
	/**
	 * Decrypts payload with the given number of layers, 0 layers = no decryption.
	 * This method is only used during authentication phase, after that only
	 * decrypt(byte[]) is used.
	 * 
	 * @param encryptedPayload
	 * @param numLayers number of encryption layers to remove. 0 = no decryption.
	 * @return the decrypted payload.
	 * @throws Exception
	 */
	protected byte[] decrypt(byte[] encryptedPayload, int numLayers) throws Exception
	{
		if(numLayers < 0)
			throw new Exception("Negative number of layers");
		
		if(numLayers == 0)
		{
			//unencrypted packet
			return encryptedPayload;
		}
			
		// Make an array containing the sessionIds in reverse order,
		// because they were encrypted in that order and onionAuth expects
		// them like that
		int[] neededSessionIds = new int[numLayers];
		for(int i=0; i < numLayers; i++)
		{
			neededSessionIds[i] = authSessionIds[numLayers-1-i];
		}
		
		//send the data to OnionAuth API and get decrypted data back.
		OnionAuthDecryptResp response = 
			Main.getOaas().AUTHLAYERDECRYPT(Main.getOaas().newOnionAuthDecrypt(authApiId, authSessionIds, encryptedPayload));
		
		return response.getPayload();
	}

	/**
	 * Authenticates via OnionAuth. Encrypts with numLayers (0 = no encryption).
	 * 
	 * @param hopHostkey the hostkey of the hop with which we want to authenticate
	 * @param numLayers the number of layers needed for that hop
	 * @return session ID for OnionAuth API - necessary for de-/encryption
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
				Main.getOaas().newOnionAuthSessionStartMessage(authApiId, Util.getHostkeyObject(hopHostkey)));
		outGoingData.writeInt(hs1.getPayload().length);
		outGoingData.write(hs1.getPayload());

		byte[] encryptedPayload = encrypt(outgoingDataBAOS.toByteArray(), numLayers);
		m.write(new OnionMessage(nextHopMId,OnionMessage.CONTROL_MESSAGE, nextHopAddress,encryptedPayload));

		// read incoming hs2 into buffer. We need to know the length of hs2
		OnionMessage incomingMessage = m.read(nextHopMId, nextHopAddress);
		byte[] hs2payload = decrypt(incomingMessage.data, numLayers);
		
		
		Main.getOaas().AUTHSESSIONINCOMINGHS2(
				Main.getOaas().newOnionAuthSessionIncomingHS2(authApiId, hs1.getSessionID(), hs2payload));

		return hs1.getSessionID();
	}

	
	/**
	 * Sends data through the tunnel, be it real VOIP or cover traffic.
	 * @param isRealData indicates if the data is real data or not (-> cover traffic)
	 * @param data the payload
	 * @throws Exception 
	 */
	public void sendData(boolean isRealData, byte[] data) throws Exception
	{
		byte[] payload = new byte[data.length + 1];
		payload[0] = isRealData ? MSG_DATA : MSG_COVER;
		
		m.write(new OnionMessage(nextHopMId, OnionMessage.CONTROL_MESSAGE, nextHopAddress, encrypt(payload)));		
	}
	
	/**
	 * Sends (real) VOIP-data
	 * @param data the payload
	 * @throws Exception
	 */
	public void sendRealData(byte[] data) throws Exception
	{
		sendData(true,data);
	}
	
	/**
	 * Sends fake/ cover traffic of the specified size
	 * @param size size of the cover traffic to generate
	 * @throws Exception
	 */
	public void sendCoverData(int size) throws Exception
	{
		byte[] rndData = new byte[size];
		new Random().nextBytes(rndData);
		sendData(false, rndData);
	}

	/**
	 * Sends a request to destroy the tunnel to all hops and tears the tunnel down.
	 * @throws Exception
	 */
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


	/**
	 * Gets the next UDP-message and processes it accordingly, depending on wheter it's real or cover traffic.
	 * @throws Exception
	 */
	public void getAndProcessNextDataMessage() throws Exception {
		
		OnionMessage incomingMessage = m.read(nextHopMId, nextHopAddress);
		// decrypt data
		byte[] payload = decrypt(incomingMessage.data);
		
		if(payload[0] != MSG_DATA)
			//ignore cover traffic
			return;
		
		Main.getOas().ONIONTUNNELDATAINCOMING(Main.getOas().newOnionTunnelDataMessage(externalID, Arrays.copyOfRange(payload, 1, payload.length)));
			
		return;
	}


}
