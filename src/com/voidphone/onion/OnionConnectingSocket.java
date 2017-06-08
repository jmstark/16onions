package com.voidphone.onion;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.voidphone.api.APIOnionAuthSocket;

/**
 * When the main application wants to connect to a new node, it establishes a
 * TCP connection to it and identifies as fellow onion node (e.g. by sending a
 * magic byte sequence). It then creates a new OnionSocket instance, passing the
 * TCP socket, destination hostkey and buffer size (how to determine?) to the
 * constructor.
 */
public class OnionConnectingSocket extends OnionBaseSocket
{

	public OnionConnectingSocket(Socket sock, byte[] hostkey, int size) throws IOException
	{
		super(sock, hostkey, size);
	}

	@Override
	void initiateOnionConnection(InputStream in, OutputStream out, byte[] hostkey, int size) throws IOException
	{
		APIOnionAuthSocket.AUTHSESSIONHS1 hs1;
		byte buffer[] = new byte[size];

		//Get hs1 from onionAuth and send it to remote peer
		//the API reply also contains a requestID and a sessionID.
		//requestID is implicitely handled but maybe we have to save
		//the session ID?
		hs1 = oasock.AUTHSESSIONSTART(new APIOnionAuthSocket.AUTHSESSIONSTART(hostkey));
		out.write(hs1.getPayload());
		// read incoming hs2 into buffer
		in.read(buffer);

		oasock.AUTHSESSIONINCOMINGHS2(new APIOnionAuthSocket.AUTHSESSIONINCOMINGHS2(hs1.getSession(), buffer));

	}

}
