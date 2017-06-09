package com.voidphone.onion;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
	
	protected static APIOnionAuthSocket oasock = null;

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
