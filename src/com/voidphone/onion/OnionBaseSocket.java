package com.voidphone.onion;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import com.voidphone.api.APIOnionAuthSocket;

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
	protected static APIOnionAuthSocket oasock = null;
	protected int tunnelId;
	protected DatagramSocket nextHopDataOutgoing = null;
	protected DatagramSocket lastHopDataOutgoing = null;	
	protected byte[] nextHopAddress = null;
	protected short nextHopPort = 0;
	protected byte[] lastHopAddress = null;
	protected short lastHopPort = 0;

	abstract void initiateOnionConnection(DataInputStream in, DataOutputStream out, byte hostkey[], int bufferSize)
			throws IOException;

	private OnionBaseSocket(DataInputStream in, DataOutputStream out, byte hostkey[], int bufferSize) throws IOException
	{
		initiateOnionConnection(in, out, hostkey, bufferSize);

	}

	public OnionBaseSocket(Socket sock, byte hostkey[], int bufferSize) throws IOException
	{
		this(new DataInputStream(sock.getInputStream()), new DataOutputStream(sock.getOutputStream()), hostkey, bufferSize);
	}

	public OnionBaseSocket(OnionBaseSocket sock, byte hostkey[], int bufferSize) throws IOException
	{
		this(sock.getDataInputStream(), sock.getDataOutputStream(), hostkey, bufferSize);
	}

	
	/**
	 * Processes the next UDP data message or forwards it,
	 * if there is a next hop. Since a node can have more
	 * than one tunnel through it, a dispatcher receives
	 * all incoming UDP packets, extracts the tunnel ID
	 * and then calls this method of the according instance.
	 * @param in
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

	public static void initAPIOnionAuthSocket(APIOnionAuthSocket oasock)
	{
		if (oasock == null)
		{
			OnionBaseSocket.oasock = oasock;
		}
	}
}
