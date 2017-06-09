package com.voidphone.onion;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

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


	public OnionListenerSocket(Socket sock, byte[] hostkey, int size) throws IOException
	{
		super(sock, hostkey, size);
	}
	
	
	/**
	 * Endless loop fetching control messages (TCP)
	 * E.g. forwarding instruction, tunnel teardown
	 */
	//TODO
	
	
	

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
