package com.voidphone.onion;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;

import com.voidphone.api.OnionAuthAPISocket;
import com.voidphone.api.OnionAuthAPISocket.AUTHSESSIONINCOMINGHS1;
import com.voidphone.api.Config;
import com.voidphone.general.General;

/**
 * Main application runs a TCP server socket. There, when it receives a new
 * connection, it then constructs an OnionServerSocket, passing the new TCP
 * socket, the remote hostkey and the buffersize (how to determine?) to the
 * constructor. Then it should send a TUNNEL_READY API message.
 * 
 */
public class OnionListenerSocket extends OnionBaseSocket implements Main.Attachable
{
	private DataOutputStream nextHopControlMsgOutgoing = null;
	private DataInputStream lastHopControlMsgIncoming = null;
	private Socket nextHopSocket = null;
	protected static DatagramSocket dataIncoming;// = new DatagramSocket(1423);


	public OnionListenerSocket(SocketChannel sock, byte[] hostkey, Config config) throws IOException
	{
		super(sock, hostkey, config);
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
			nextHopDataOutgoing = null;

			lastHopAddress = null;
			lastHopPort = 0;
			lastHopDataOutgoing.close();
			lastHopDataOutgoing = null;
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
				nextHopDataOutgoing = new DatagramSocket();
				nextHopDataOutgoing.connect(InetAddress.getByAddress(nextHopAddress), nextHopPort);

			}

	}

	@Override
	void initiateOnionConnection(DataInputStream in, DataOutputStream out, byte[] hostkey, Config config) throws IOException
	{
		OnionAuthAPISocket.AUTHSESSIONHS2 hs2;
		short size;
		byte buffer[];
		
		if (in.readInt() != MAGIC_SEQ_CONNECTION_START | in.readInt() != VERSION)
			throw new IOException("Tried to connect with non-onion node or wrong version node");

		// read incoming hs1 from remote peer into buffer
		size = in.readShort();
		buffer = new byte[size];
		in.readFully(buffer, 0, size);

		// get hs2 from onionAuth and send it back to remote peer
		hs2 = config.getOnionAuthAPISocket().AUTHSESSIONINCOMINGHS1(new AUTHSESSIONINCOMINGHS1(hostkey, buffer));
		out.writeShort(hs2.getPayload().length);
		out.write(hs2.getPayload());
	}

	@Override
	public boolean handle() {
		try {
			System.out.println(dis.read());
			System.out.println(dis.read());
			System.out.println(dis.read());
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.exit(1);
		return false;
	}
}
