package com.voidphone.onion;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;

import com.voidphone.api.Config;

/**
 * Base class for OnionConnectingSocket and OnionListenerSocket
 * 
 */
public abstract class OnionBaseSocket
{

	public final static int MAGIC_SEQ_CONNECTION_START = 0x7af3bef1;
	public final static int VERSION = 1;
	protected final byte MSG_BUILD_TUNNEL = 0xb;
	protected final byte MSG_DESTROY_TUNNEL = 0xd;
	protected final int CONTROL_PACKET_SIZE = 8192;
	protected short[] authSessionIds;
	protected Config config;
	
	protected static long apiRequestCounter = 1;
	
	
	// buffer is used to construct packets before encrypting/decrypting 
	// and sending/receiving them via dos or dis.
	// Using it makes sure that always same-sized packets are sent and received.
	protected ByteBuffer buffer = ByteBuffer.allocate(CONTROL_PACKET_SIZE);
	
	
	/**
	 * Encrypts payload with the given number of layers, 0 layers = no encryption.
	 * 
	 * @param numLayers
	 * @return encrypted payload (or plaintext if numLayers == 0)
	 * @throws Exception 
	 */
	protected byte[] encrypt(byte[] payload, int numLayers) throws Exception
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
	protected byte[] decrypt(byte[] encryptedPayload, int numLayers) throws Exception
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
	
	/*		


	protected Config config = null;
	protected final int MAX_DATA_PACKET_SIZE = 65536/2;
	protected int tunnelId = 0;
	protected DatagramSocket nextHopDataOutgoing = null;
	protected DatagramSocket lastHopDataOutgoing = null;	
	protected byte[] nextHopAddress = null;
	protected short nextHopPort = 0;
	protected byte[] lastHopAddress = null;
	protected short lastHopPort = 0;
	protected DataInputStream dis;
	protected DataOutputStream dos;
	protected byte[] destHostkey;
	abstract void authenticate() throws IOException;

	protected OnionBaseSocket(Config c) {
		config = c;
	}
*/	
	/**
	 * Processes the next UDP data message or forwards it,
	 * if there is a next hop. Since a node can have more
	 * than one tunnel through it, a dispatcher receives
	 * all incoming UDP packets, extracts the tunnel ID
	 * and then calls this method of the according instance.
	 * @param dis
	 * @throws IOException 
	 */
	/*
	void processNextDataMessage(DatagramPacket incomingPacket) throws IOException
	{
		DatagramSocket forwardTarget = null;
		if(incomingPacket.getAddress().getAddress().equals(lastHopAddress) && incomingPacket.getPort() == lastHopPort)
		{
			forwardTarget = nextHopDataOutgoing;
		}
		else if(incomingPacket.getAddress().getAddress().equals(nextHopAddress) && incomingPacket.getPort() == nextHopPort)
		{
			forwardTarget = lastHopDataOutgoing;
		}
		
		if(forwardTarget == null)
		{
			//no forwarding, this packet is for us
			//TODO: forward data to API
		}
		else
		{
			//forward data to next node
			forwardTarget.send(incomingPacket);
		}

	}
*/
	
}
