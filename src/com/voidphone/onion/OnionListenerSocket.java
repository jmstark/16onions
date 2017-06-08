package com.voidphone.onion;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.voidphone.api.APIOnionAuthSocket;
import com.voidphone.api.APIOnionAuthSocket.AUTHSESSIONINCOMINGHS1;

/** Main application runs a TCP server socket. There, when it receives a 
 * new connection, it checks if it's actually an onion connection (e.g. by
 * checking for magic sequence) and not a different application. 
 * If yes, it then constructs an OnionServerSocket, passing the new 
 * TCP socket, the remote hostkey and the buffersize (how to determine?)
 *  to the constructor. Then it should send a TUNNEL_READY API message.
 */
public class OnionListenerSocket extends OnionBaseSocket
{


	public OnionListenerSocket(Socket sock, byte[] hostkey, int size) throws IOException
	{
		super(sock, hostkey, size);
	}

	@Override
	void initiateOnionConnection(InputStream in, OutputStream out, byte[] hostkey, int size) throws IOException
	{
		APIOnionAuthSocket.AUTHSESSIONHS2 hs2;
		byte[] buffer = new byte[size];
		
		//read incoming hs1 from remote peer into buffer
		in.read(buffer);
		
		//get hs2 from onionAuth and send it back to remote peer
		//the API reply also contains a requestID and a sessionID.
		//requestID is implicitely handled but maybe we have to save
		//the session ID?
		hs2 = oasock.AUTHSESSIONINCOMINGHS1(new AUTHSESSIONINCOMINGHS1(hostkey, buffer));
		out.write(hs2.getPayload());
		
	}

}
