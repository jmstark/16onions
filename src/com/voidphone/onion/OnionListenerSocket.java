package com.voidphone.onion;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

import sun.net.InetAddressCachePolicy;

import com.voidphone.api.APIOnionAuthSocket;
import com.voidphone.api.APIOnionAuthSocket.AUTHSESSIONINCOMINGHS1;

/**
 * Main application runs a TCP server socket. There, when it receives a new
 * connection, it then constructs an OnionServerSocket, passing the new TCP
 * socket, the remote hostkey and the buffersize (how to determine?) to the
 * constructor. Then it should send a TUNNEL_READY API message.
 * 
 */
public class OnionListenerSocket extends OnionBaseSocket
{

	private byte[] ownAddress = { 0, 0, 0, 0 };
	private short ownPort = 0;
	private DataOutputStream nextHopControlMsgOutgoing = null;
	private DataInputStream lastHopControlMsgIncoming = null;
	private Socket nextHopSocket = null;
	protected static DatagramSocket dataIncoming;// = new DatagramSocket(1423);


	public OnionListenerSocket(Socket sock, byte[] hostkey, int size) throws IOException
	{
		super(sock, hostkey, size);
	}

	/**
	 * This function is used to build and destroy tunnels. Building/destroying
	 * is done iteratively. A control message only consists of message type,
	 * address length (4 or 6, depending on IP version), IP address and port
	 * number (same port number for TCP and UDP). The function receives the
	 * message, and if the next hop is already known the message gets forwarded
	 * there, if not, the next hop is constructed.
	 * 
	 * @throws IOException
	 */
	void processNextControlMessage() throws Exception
	{
		byte messageType = lastHopControlMsgIncoming.readByte();
		if (messageType == MSG_DESTROY_TUNNEL)
		{
			//forward message
			nextHopControlMsgOutgoing.write(messageType);
			
			// tear down tunnel
			nextHopControlMsgOutgoing.close();
			nextHopSocket = null;
			nextHopControlMsgOutgoing = null;
			nextHopAddress = null;
			nextHopPort = 0;
			nextHopDataOutgoing.close();

			lastHopAddress = null;
			lastHopPort = 0;
			lastHopDataOutgoing.close();
		}
		// construct next hop
		byte destinationAddressLength = lastHopControlMsgIncoming.readByte();
		byte[] destinationAddress = new byte[destinationAddressLength];
		lastHopControlMsgIncoming.readFully(destinationAddress);
		short destinationPort = lastHopControlMsgIncoming.readShort();

		if (nextHopSocket != null)
		{
			// forward data to next hop
			nextHopControlMsgOutgoing.write(messageType);
			nextHopControlMsgOutgoing.write(destinationAddressLength);
			nextHopControlMsgOutgoing.write(destinationAddress);
			nextHopControlMsgOutgoing.write(destinationPort);


		}
		else
			if (messageType == MSG_BUILD_TUNNEL)
			{
				// connect to next hop
				nextHopSocket = new Socket(InetAddress.getByAddress(destinationAddress), destinationPort);
				nextHopControlMsgOutgoing = new DataOutputStream(nextHopSocket.getOutputStream());
				nextHopAddress = destinationAddress.clone();
				nextHopPort = destinationPort;
				nextHopDataOutgoing.connect(InetAddress.getByAddress(nextHopAddress), nextHopPort);

			}

	}

	@Override
	void initiateOnionConnection(DataInputStream in, DataOutputStream out, byte[] hostkey, int size) throws IOException
	{

		APIOnionAuthSocket.AUTHSESSIONHS2 hs2;
		byte[] buffer = new byte[size];

		if (in.readInt() != MAGIC_SEQ_CONNECTION_START || in.readInt() != VERSION)
			throw new IOException("Tried to connect with non-onion node or wrong version node");

		// read incoming hs1 from remote peer into buffer
		// TODO: replace 1234 with actual hs1 length
		in.readFully(buffer, 0, 1234);

		// get hs2 from onionAuth and send it back to remote peer
		// the API reply also contains a requestID and a sessionID.
		// requestID is implicitely handled but maybe we have to save
		// the session ID?
		hs2 = oasock.AUTHSESSIONINCOMINGHS1(new AUTHSESSIONINCOMINGHS1(hostkey, buffer));
		out.write(hs2.getPayload());

	}

}
