package com.voidphone.onion;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.channels.SocketChannel;

import com.voidphone.api.Config;
import com.voidphone.api.OnionAuthAPISocket;

/**
 * When the main application wants to connect to a new node, it establishes a
 * TCP connection to it and then creates a new OnionSocket instance, passing the
 * TCP socket, destination hostkey and buffer size (how to determine?) to the
 * constructor.
 */
public class OnionConnectingSocket extends OnionBaseSocket
{
	
	public OnionConnectingSocket(SocketChannel sock, byte[] hostkey, Config config) throws IOException
	{
		super(sock, hostkey, config);
	}

	@Override
	void initiateOnionConnection(DataInputStream in, DataOutputStream out, byte[] hostkey, Config config) throws IOException
	{
		OnionAuthAPISocket.AUTHSESSIONHS1 hs1;
		short size;
		byte buffer[];
		
		out.writeInt(MAGIC_SEQ_CONNECTION_START);
		out.writeInt(VERSION);

		// get hs1 from onionAuth and send it to remote peer
		hs1 = config.getOnionAuthAPISocket().AUTHSESSIONSTART(new OnionAuthAPISocket.AUTHSESSIONSTART(hostkey));
		out.writeShort(hs1.getPayload().length);
		out.write(hs1.getPayload());
		
		// read incoming hs2 into buffer. We need to know the length of hs2
		size = in.readShort();
		buffer = new byte[size];
		in.readFully(buffer, 0, size);

		config.getOnionAuthAPISocket().AUTHSESSIONINCOMINGHS2(new OnionAuthAPISocket.AUTHSESSIONINCOMINGHS2(hs1.getSession(), buffer));
	}
	
	void insertNewHop(byte[] address, short port, byte[] hostKey)
	{
		
	}

}
