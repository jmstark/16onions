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
	
	protected InetSocketAddress destAddr;
	protected DataInputStream dis;
	protected DataOutputStream dos;
	public final int externalID;
	protected byte[] destHostkey;

	
	/**
	 * Constructor
	 * 
	 * @param destAddr the address of the target node (i.e. the last hop)
	 * @param destHostkey the hostkey of the target node
	 * @param config configuration
	 * @param hopCount the number of intermediate hops (excluding our node and the target node)
	 * @param externalID Other modules use this ID to refer to this tunnel (backup tunnels with the 
	 * same end destination may get the same ID)
	 * @throws Exception 
	 */
	public OnionConnectingSocket(InetSocketAddress destAddr, byte[] destHostkey, Config config, int hopCount, int externalID) throws Exception
	{
		this.externalID = externalID;
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
		//If end node is unspecified, use another random node
		if(destAddr == null || destHostkey == null)
			hops[hopCount] = config.getRPSAPISocket().RPSQUERY();
		else
			hops[hopCount] = new OnionPeer(destAddr, destHostkey);
		
		// Connect to first hop - all other connections are forwarded over this hop
		Socket nextHopSocket = new Socket(hops[0].getAddress().getAddress(),hops[0].getAddress().getPort());
		dis = new DataInputStream(nextHopSocket.getInputStream());
		dos = new DataOutputStream(nextHopSocket.getOutputStream());
		authSessionIds[0] = authenticate(hops[0].getHostkey(), 0);

		
		//Establish forwardings (if any)
		for(int i = 1; i < hops.length; i++)
		{
			// Send forwarding request to node i-1 -> 
			// it connects to the next node i and then forwards there
			// everything it receives from that point on.

			//First, send the actual request along with the target address. Encrypted.
			buffer.clear();
			buffer.putShort(MSG_BUILD_TUNNEL);
			byte[] rawAddress = hops[i].getAddress().getAddress().getAddress();
			buffer.put((byte)rawAddress.length);
			buffer.put(rawAddress);
			buffer.putShort((short)hops[i].getAddress().getPort());
			byte[] encryptedPayload = encrypt(buffer.array(),i);
			dos.writeShort(encryptedPayload.length);
			dos.write(encryptedPayload);
			dos.flush();
			
			//Now, we are indirectly connected to the target node. Authenticate to that node.
			authSessionIds[i] = authenticate(hops[i].getHostkey(), i);
		}
	}

	/**
	 * 
	 * 
	 * @param destAddr the address of the target node (i.e. the last hop)
	 * @param destHostkey the hostkey of the target node
	 * @param config configuration
	 * @param hopCount the number of intermediate hops (excluding our node and the target node)
	 * @throws Exception 
	 */
	public OnionConnectingSocket(InetSocketAddress destAddr, byte[] destHostkey, Config config) throws Exception
	{
		this(destAddr,destHostkey,config,config.getHopCount(),new Double(Math.random()).hashCode());
		
	}

	
	/**
	 * 
	 * 
	 * @param destAddr the address of the target node (i.e. the last hop)
	 * @param destHostkey the hostkey of the target node
	 * @param config configuration
	 * @param hopCount the number of intermediate hops (excluding our node and the target node)
	 * @throws Exception 
	 */
	public OnionConnectingSocket(InetSocketAddress destAddr, byte[] destHostkey, Config config, int externalID) throws Exception
	{
		this(destAddr,destHostkey,config,config.getHopCount(),externalID);
		
	}
	
	/**
	 * Authenticates via OnionAuth. Encrypts with numLayers (0 = no encryption).
	 * 
	 * @param hopHostkey
	 * @param numLayers
	 * @return sessionID
	 * @throws Exception
	 */
	public short authenticate(byte[] hopHostkey, int numLayers) throws Exception
	{

		OnionAuthAPISocket.AUTHSESSIONHS1 hs1;
		
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
	
	public void destroy()
	{
		// TODO
	}

}
