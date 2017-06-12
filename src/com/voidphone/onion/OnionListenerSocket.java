package com.voidphone.onion;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

import sun.net.InetAddressCachePolicy;

import com.voidphone.api.APIOnionAuthSocket;
import com.voidphone.api.APIOnionAuthSocket.AUTHSESSIONINCOMINGHS1;

/** Main application runs a TCP server socket. There, when it receives a 
 * new connection, it then constructs an OnionServerSocket, passing the new 
 * TCP socket, the remote hostkey and the buffersize (how to determine?)
 *  to the constructor. Then it should send a TUNNEL_READY API message.
 *  
 */
public class OnionListenerSocket extends OnionBaseSocket
{

	private byte[] ownAddress = {0,0,0,0};
	private byte[] nextHopAddress = null;
	byte nextHopPort = 0;
	private DataOutputStream nextHopControlMsgOut = null;

	public OnionListenerSocket(Socket sock, byte[] hostkey, int size) throws IOException
	{
		super(sock, hostkey, size);
	}
	
	
	/**
	 * Function handling TCP control messages. 
	 * Those are used for building tunnels.
	 * @param in
	 * @param out
	 * @throws IOException 
	 */
	void processNextControlMessage(DataInputStream in, DataOutputStream out) throws Exception
	{
		byte recipientAddressLength = in.readByte();
		byte[] recipient = new byte[recipientAddressLength];
		in.readFully(recipient);
		byte recipientPort = in.readByte();
		if(recipient.equals(ownAddress))
		{
			if(nextHopAddress != null)
				throw new Exception("nextHop already defined; don't know what to do");
			//this can only mean that we must add a new hop
			//since there are no other control messages.
			byte nextHopAddressLength = in.readByte();
			nextHopAddress = new byte[nextHopAddressLength];
			in.readFully(nextHopAddress);	
			nextHopPort = in.readByte();
			Socket nextHopSocket = new Socket(InetAddress.getByAddress(nextHopAddress),nextHopPort);
			nextHopControlMsgOut = new DataOutputStream(nextHopSocket.getOutputStream());
		}
		else
		{
			//forward the message to next hop
			nextHopControlMsgOut.write(recipientAddressLength);
			nextHopControlMsgOut.write()
		}
		
		
	}
	
	
	void processNextDataMessage(DataInputStream in, DataOutputStream out)
	{
		
	}
	
	

	@Override
	void initiateOnionConnection(DataInputStream in, DataOutputStream out, byte[] hostkey, int size) throws IOException
	{

		
		APIOnionAuthSocket.AUTHSESSIONHS2 hs2;
		byte[] buffer = new byte[size];
		
		if(in.readInt() != MAGIC_SEQ_CONNECTION_START || in.readInt() != VERSION)
			throw new IOException("Tried to connect with non-onion node or wrong version node");
		
		
		//read incoming hs1 from remote peer into buffer
		//TODO: replace 1234 with actual hs1 length
		in.readFully(buffer, 0, 1234);
		
		//get hs2 from onionAuth and send it back to remote peer
		//the API reply also contains a requestID and a sessionID.
		//requestID is implicitely handled but maybe we have to save
		//the session ID?
		hs2 = oasock.AUTHSESSIONINCOMINGHS1(new AUTHSESSIONINCOMINGHS1(hostkey, buffer));
		out.write(hs2.getPayload());
		
	}

}
