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
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

import com.voidphone.general.AuthApiException;
import com.voidphone.general.General;
import com.voidphone.general.IllegalAddressException;
import com.voidphone.general.IllegalIDException;
import com.voidphone.general.NoRpsPeerException;
import com.voidphone.general.OnionAuthErrorException;
import com.voidphone.general.SizeLimitExceededException;
import com.voidphone.general.TunnelCrashException;
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
	//Array holding all hops, including the tunnel end
	protected RpsPeerMessage[] hops;
	OnionAuthSessionHS1 hs1;



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
	public OnionConnectingSocket(Multiplexer m, InetSocketAddress destAddr, byte[] destHostkey, int onionApiId) throws Exception {
		super(m, onionApiId);
		int hopCount = Main.getConfig().hopCount;
		this.destAddr = destAddr;
		this.destHostkey = destHostkey;
		authSessionIds = new int[hopCount + 1];
		rpsApiId = Main.getRas().register();

		// Fill up an array with intermediate hops and the target node
		hops = new RpsPeerMessage[hopCount + 1];
		RpsPeerMessage rpsMsg;
		// If end node is unspecified (null), use another random node
		for (int i = 0, retries = 0; i < hopCount + (destAddr == null || destHostkey == null ? 1 : 0 ) ; i++) {
			rpsMsg = Main.getRas().RPSQUERY(Main.getRas().newRpsQueryMessage(rpsApiId));
			if(rpsMsg == null)
				throw new NoRpsPeerException();
			
			if(Arrays.asList(hops).contains(rpsMsg) || rpsMsg.getAddress().equals(destAddr))
			{
				retries++;
				if(retries > 10 * hopCount)
				{
					General.error("Not enough RPS peers");
					throw new NoRpsPeerException();
				}
				i--;
				continue;
			}
				
			hops[i] = rpsMsg;
		}
		
		if (destAddr != null && destHostkey != null)
			hops[hopCount] = new RpsPeerMessage(destAddr, Util.getHostkeyObject(destHostkey));

		nextHopAddress = hops[0].getAddress();
		
		// Connect to first hop - all other connections are forwarded over this hop
		m.registerAddress(hops[0].getAddress());
		nextHopMId = m.registerID(hops[0].getAddress());


	}
	
	/**
	 * Alternative constructor - a new onionApiId is assigned implicitely
	 * 
	 * @param m The multiplexer over which we communicate with other peers
	 * @param destAddr the address of the target node (i.e. the last hop)
	 * @param destHostkey the hostkey of the target node (i.e. the last hop)
	 * @throws Exception
	 */
	public OnionConnectingSocket(Multiplexer m, InetSocketAddress destAddr, byte[] destHostkey) throws SizeLimitExceededException, Exception
	{
		this(m,destAddr,destHostkey,Main.getOas().register());
	}
	
	public void constructTunnel(short newNextHopMId, InetSocketAddress newNextHopAddress) throws Exception
	{
		nextHopMId = newNextHopMId;
		nextHopAddress = newNextHopAddress;
		constructTunnel();
	}
	
	
	/**
	 * This method must be called after the constructor has returned
	 * @throws Exception 
	 */
	public void constructTunnel() throws Exception
	{
		General.info("Connected to hop 0");
		
		General.info("authenticating to hop 0, address: " + hops[0].getAddress());

		//beginAuthentication(hops[0].hostkey, 0);
		authSessionIds[0] = finishAuthentication(Util.getHostkeyBytes(hops[0].getHostkey()), 0);

		General.info("authenticated to hop 0, address: " + hops[0].getAddress());
		
		// Establish forwardings (if any)
		for (int i = 1; i < hops.length; i++) {
			// Send forwarding request to node i-1 ->
			// it connects to the next node i and then forwards there
			// everything it receives from that point on.

			// First, send the actual request along with the target address. Encrypted.
			ByteArrayOutputStream outgoingDataBAOS = new ByteArrayOutputStream();
			DataOutputStream outgoingData = new DataOutputStream(outgoingDataBAOS);
			
			outgoingData.write(MSG_BUILD_TUNNEL);
			byte[] rawAddress = hops[i].getAddress().getAddress().getAddress();
			outgoingData.write((byte) rawAddress.length);
			outgoingData.write(rawAddress);
			outgoingData.writeInt(hops[i].getAddress().getPort());
			byte[] encryptedPayload = encrypt(outgoingDataBAOS.toByteArray(), i);

			General.info("Sending tunnel building request to hop " + (i-1) + ", address: " + hops[i-1].getAddress() );
			
			m.write(new OnionMessage(nextHopMId, OnionMessage.CONTROL_MESSAGE, nextHopAddress, encryptedPayload));
			
			General.info("Sent tunnel building request to hop " + (i-1) + ", address: " + hops[i-1].getAddress() );
			
			// Now, we are indirectly connected to the target node. 
			// Authenticate to that node.
			beginAuthentication(Util.getHostkeyBytes(hops[i].getHostkey()), i);
			
			General.info("authenticating to hop " + i  + ", address: " + hops[i].getAddress());
			
			authSessionIds[i] = finishAuthentication(Util.getHostkeyBytes(hops[i].getHostkey()), i);
			
			General.info("authenticated to hop " + i  + ", address: " + hops[i].getAddress());
			

		}
		
		//Signal to the last hop, that it is the target so it can signal an incoming tunnel to the CM
		ByteArrayOutputStream outgoingDataBAOS = new ByteArrayOutputStream();
		DataOutputStream outgoingData = new DataOutputStream(outgoingDataBAOS);
		outgoingData.writeShort(MSG_INCOMING_TUNNEL);
		outgoingData.writeInt(onionApiId);
		m.write(new OnionMessage(nextHopMId, OnionMessage.CONTROL_MESSAGE, nextHopAddress, encrypt(outgoingDataBAOS.toByteArray())));
		
		General.info("Signaled tunnel end to last hop");
		
		Main.getOas().addActiveTunnel(this);
		//Inform our CM, that the requested tunnel is ready.
		Main.getOas().ONIONTUNNELREADY(Main.getOas().newOnionTunnelReadyMessage(onionApiId, destHostkey));
		
		General.info("Signalled successful tunnel build to CM");
	}
	
	
	public short getNextHopMId()
	{
		return nextHopMId;
	}
	
	public short detachId() throws IllegalAddressException, IllegalIDException
	{
		m.detachID(nextHopMId,nextHopAddress);
		return nextHopMId;
	}
	
	
	
	/**
	 * Encrypts payload for end hop.
	 * @param payload the plaintext payload
	 * @return encrypted payload
	 * @throws AuthApiException 
	 * @throws Exception
	 */
	protected byte[] encrypt(byte[] payload) throws AuthApiException {
		return encrypt(payload, authSessionIds.length);
	}

	/**
	 * Decrypts payload of end hop.
	 * 
	 * @param payload the encrypted payload
	 * @return Decrypted payload
	 * @throws AuthApiException 
	 * @throws Exception
	 */
	protected byte[] decrypt(byte[] payload) throws AuthApiException {
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
	 * @throws AuthApiException 
	 * @throws Exception 
	 */
	protected byte[] encrypt(byte[] payload, int numLayers) throws AuthApiException
	{
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
		OnionAuthEncryptResp response;
		try {
			response = Main.getOaas().AUTHLAYERENCRYPT(Main.getOaas().newOnionAuthEncrypt(authApiId, neededSessionIds, payload));
		} catch (InterruptedException | IllegalIDException | OnionAuthErrorException e) {
			General.error("AUTH API communication error");
			throw new AuthApiException();
		}


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
	 * @throws AuthApiException 
	 * @throws Exception
	 */
	protected byte[] decrypt(byte[] encryptedPayload, int numLayers) throws AuthApiException
	{
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
		OnionAuthDecryptResp response;
		try {
			response = Main.getOaas().AUTHLAYERDECRYPT(Main.getOaas().newOnionAuthDecrypt(authApiId, neededSessionIds, encryptedPayload));
		} catch (InterruptedException | IllegalIDException | OnionAuthErrorException e) {
			General.error("AUTH API communication error");
			throw new AuthApiException();
		}
		
		return response.getPayload();
	}

	
	public void beginAuthentication(byte[] hopHostkey, int numLayers) throws Exception
	{
		if(hopHostkey == null)
			hopHostkey = Util.getHostkeyBytes(hops[0].getHostkey());
		ByteArrayOutputStream outgoingDataBAOS = new ByteArrayOutputStream();
		DataOutputStream outGoingData = new DataOutputStream(outgoingDataBAOS);
		
		outGoingData.writeInt(MAGIC_SEQ_CONNECTION_START);
		outGoingData.writeInt(VERSION);

		// get hs1 from onionAuth and send it to remote peer

		hs1 = Main.getOaas().AUTHSESSIONSTART(
				Main.getOaas().newOnionAuthSessionStartMessage(authApiId, Util.getHostkeyObject(hopHostkey)));
		outGoingData.writeInt(hs1.getPayload().length);
		outGoingData.write(hs1.getPayload());

		byte[] encryptedPayload = encrypt(outgoingDataBAOS.toByteArray(), numLayers);
		m.write(new OnionMessage(nextHopMId,OnionMessage.CONTROL_MESSAGE, nextHopAddress,encryptedPayload));
	}
	
	
	/**
	 * Authenticates via OnionAuth. Encrypts with numLayers (0 = no encryption).
	 * 
	 * @param hopHostkey the hostkey of the hop with which we want to authenticate
	 * @param numLayers the number of layers needed for that hop
	 * @return session ID for OnionAuth API - necessary for de-/encryption
	 * @throws Exception
	 */
	public int finishAuthentication(byte[] hopHostkey, int numLayers) throws Exception {


		
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
	 * @throws TunnelCrashException 
	 * @throws Exception 
	 */
	@Override	
	public void sendData(boolean isRealData, byte[] data) throws TunnelCrashException 
	{
		sendData(isRealData, data, nextHopMId, nextHopAddress);
	}

	/**
	 * Sends a request to destroy the tunnel to all hops and tears the tunnel down.
	 * @throws Exception
	 */
	public void destroy() throws Exception {
		byte[] plainMsg = new byte[] {MSG_DESTROY_TUNNEL};
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
		
		//Handle timeouts
		if(incomingMessage == null)
		{
			if(heartbeatRetries>5)
			{
				throw new IOException("Tunnel timeout: no data received");				
			}
			//send heartbeat
			General.info("Tunnel still alive? Sending heartbeat, expecting some answer (e.g. cover traffic)");
			sendHeartbeat(nextHopMId, nextHopAddress);;
			heartbeatRetries++;
			return;
		}

		//No timeout, so we can reset the counter
		heartbeatRetries = 0;


		
		// decrypt data
		byte[] payload = decrypt(incomingMessage.data);
		
		if(payload[0] == MSG_HEARTBEAT)
		{
			//respond with some data so the other end knows we're alive
			General.info("Heartbeat received, responding with some data");
			sendCoverData(32);
			return;
		}	
		
		if(payload[0] != MSG_DATA)
		{
			//ignore cover traffic
			General.info("Cover traffic received");
			return;
		}
		
		Main.getOas().ONIONTUNNELDATAINCOMING(Main.getOas().newOnionTunnelDataMessage(onionApiId, Arrays.copyOfRange(payload, 1, payload.length)));
			
		return;
	}


}
