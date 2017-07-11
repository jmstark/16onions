package com.voidphone.onion;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;

import com.voidphone.api.Config;
import com.voidphone.api.OnionAuthAPISocket;

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

	protected final int MAX_DATA_PACKET_SIZE = 65536/2;
	protected int tunnelId;
	protected OnionAuthAPISocket onionAuthAPISocket = null;
	protected DatagramSocket nextHopDataOutgoing = null;
	protected DatagramSocket lastHopDataOutgoing = null;	
	protected byte[] nextHopAddress = null;
	protected short nextHopPort = 0;
	protected byte[] lastHopAddress = null;
	protected short lastHopPort = 0;
	protected DataInputStream dis;
	protected DataOutputStream dos;

	abstract void initiateOnionConnection(DataInputStream in, DataOutputStream out, byte hostkey[], Config config)
			throws IOException;

	private OnionBaseSocket(DataInputStream in, DataOutputStream out, byte hostkey[], Config config) throws IOException
	{
		dis = in;
		dos = out;
		initiateOnionConnection(in, out, hostkey, config);
	}

	public OnionBaseSocket(SocketChannel sock, byte hostkey[], Config config) throws IOException
	{
		this(new DataInputStream(Channels.newInputStream(sock)), new DataOutputStream(Channels.newOutputStream(sock)), hostkey, config);
	}

	public OnionBaseSocket(OnionBaseSocket sock, byte hostkey[], Config config) throws IOException
	{
		this(sock.getDataInputStream(), sock.getDataOutputStream(), hostkey, config);
	}

	
	/**
	 * Processes the next UDP data message or forwards it,
	 * if there is a next hop. Since a node can have more
	 * than one tunnel through it, a dispatcher receives
	 * all incoming UDP packets, extracts the tunnel ID
	 * and then calls this method of the according instance.
	 * @param dis
	 * @throws IOException 
	 */
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
	
	
	public DataInputStream getDataInputStream()
	{
		return null;
	}

	public DataOutputStream getDataOutputStream()
	{
		return null;
	}
}
