package com.voidphone.onion;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;


import com.voidphone.api.Config;
import com.voidphone.api.OnionAuthAPISocket;
import com.voidphone.api.RPSAPISocket;
import com.voidphone.api.RPSAPISocket.OnionPeer;

import auth.api.*;

/**
 * When the main application wants to build a tunnel to some node, it creates an
 * instance of this class, passing destination address and hostkey and the
 * hopcount to the constructor.
 */
public class OnionConnectingSocket extends OnionBaseSocket
{
	
	protected Config config;
	protected InetSocketAddress destAddr;
	protected DataInputStream dis;
	protected DataOutputStream dos;
	protected byte[] destHostkey;
	protected short[] authSessionIds;
	
	/**
	 * 
	 * @param destAddr the address of the target node (i.e. the last hop)
	 * @param destHostkey the hostkey of the target node
	 * @param config configuration
	 * @param hopCount the number of intermediate hops (excluding our node and the target node)
	 * @throws Exception 
	 */
	public OnionConnectingSocket(InetSocketAddress destAddr, byte[] destHostkey, Config config, int hopCount) throws Exception
	{
		this.config = config;
		this.destAddr = destAddr;
		this.destHostkey = destHostkey;
		authSessionIds = new short[hopCount + 1];

		// Fill up an array with intermediate hops and the target node
		OnionPeer[] hops = new OnionPeer[hopCount + 1];
		for(int i = 0; i < hopCount; i++)
		{
			hops[i] = config.getRPSAPISocket().RPSQUERY();
		}
		hops[hopCount] = new OnionPeer(destAddr, destHostkey);
		
		// Connect to first hop - all other connections are forwarded over this hop
		Socket nextHopSocket = new Socket(hops[0].getAddress().getAddress(),hops[0].getAddress().getPort());
		dis = new DataInputStream(nextHopSocket.getInputStream());
		dos = new DataOutputStream(nextHopSocket.getOutputStream());
		authSessionIds[0] = authenticate(hops[0].getHostkey(), 0);

		
		//Establish forwardings (if any)
		for(int i = 1; i < hops.length; i++)
		{
			// Send forwarding request to previous node - 
			// it connects to the next node and then forwards
			// everything it receives from that point on.
			//TODO: sendForwardingRequest(originator = hops[i-1], target = hops[i], encryptionLayers = i-1);
			authenticate(hops[i].getHostkey(), i);
		}
	}
	
	/**
	 * Encrypts payload with the given number of layers, 0 layers = no encryption.
	 * 
	 * @param numLayers
	 * @return encrypted payload (or plaintext if numLayers == 0)
	 * @throws Exception 
	 */
	private byte[] encrypt(byte[] payload, int numLayers) throws Exception
	{
		if(numLayers < 0)
			throw new Exception("Negative number of layers");
		
		if(numLayers == 0)
			return payload;

		// Make a byte array containing the sessionIds in reverse order,
		// so that each hop can "peel off" one layer
		DataOutputStream sessionIds = new DataOutputStream(new ByteArrayOutputStream());
		for(int i=0; i < numLayers; i++)
		{
			sessionIds.writeShort(authSessionIds[numLayers - i - 1]);
		}
		sessionIds.flush();
		
		//TODO: send the data to OnionAuth API and get encrypted data back.
		//Needed:
		//the byte[] or short[] inside sessionIds; payload

		return null;
	}
	
	/**
	 * Decrypts payload with the given number of layers, 0 layers = no decryption.
	 * 
	 * @param encryptedPayload
	 * @param numLayers number of encryption layers to remove.
	 * @return the decrypted payload.
	 * @throws Exception
	 */
	private byte[] decrypt(byte[] encryptedPayload, int numLayers) throws Exception
	{
		if(numLayers < 0)
			throw new Exception("Negative number of layers");
		
		if(numLayers == 0)
			return encryptedPayload;

		// Make a byte array containing the sessionIds in reverse order,
		// because they were encrypted in that order and onionAuth expects
		// them like that
		DataOutputStream sessionIds = new DataOutputStream(new ByteArrayOutputStream());
		for(int i=0; i < numLayers; i++)
		{
			sessionIds.writeShort(authSessionIds[numLayers - i - 1]);
		}
		sessionIds.flush();
		
		//TODO: send the data to OnionAuth API and get decrypted data back.
		//Needed:
		//the byte[] or short[] inside sessionIds; payload

		return null;
			
	}

	/**
	 * Authenticates via OnionAuth. Encrypts with numLayers (0 = no encryption).
	 * 
	 * @param hopHostkey
	 * @param numLayers
	 * @return sessionID
	 * @throws Exception
	 */
	short authenticate(byte[] hopHostkey, int numLayers) throws Exception
	{

		OnionAuthAPISocket.AUTHSESSIONHS1 hs1;
		short size;
		
		buffer.clear();
				
		buffer.putInt(MAGIC_SEQ_CONNECTION_START);
		buffer.putInt(VERSION);
		
		// put own public key into packet
		buffer.putShort((short)config.getHostkey().length);
		buffer.put(config.getHostkey());
		
		// get hs1 from onionAuth and send it to remote peer
		hs1 = config.getOnionAuthAPISocket().AUTHSESSIONSTART(new OnionAuthAPISocket.AUTHSESSIONSTART(hopHostkey));
		buffer.putShort((short)hs1.getPayload().length);
		buffer.put(hs1.getPayload());
		
		// we need to send the size before the encrypted packets itself because we
		// don't know the sizes that OnionAuth produces, even if
		// they should be always the same
		byte[] encryptedPayload = encrypt(buffer.array(), numLayers);
		dos.writeShort(encryptedPayload.length);
		dos.write(encryptedPayload);
		dos.flush();
		
		buffer.clear();
		
		
		// read incoming hs2 into buffer. We need to know the length of hs2
		dis.readFully(buffer.array());
		buffer.put(decrypt(buffer.array(), numLayers));
		byte[] hs2payload = new byte[buffer.getShort()];
		dis.readFully(hs2payload);
		
		buffer.clear();
		
		config.getOnionAuthAPISocket().AUTHSESSIONINCOMINGHS2(new OnionAuthAPISocket.AUTHSESSIONINCOMINGHS2(hs1.getSession(), hs2payload));
		
		return hs1.getSession();
	}

}
