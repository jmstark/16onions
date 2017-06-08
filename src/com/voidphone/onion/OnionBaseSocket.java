package com.voidphone.onion;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.voidphone.api.APIOnionAuthSocket;

/**
 * Base class for OnionConnectingSocket and OnionListenerSocket
 * 
 */
public abstract class OnionBaseSocket
{
	protected static APIOnionAuthSocket oasock = null;

	abstract void initiateOnionConnection(InputStream in, OutputStream out, byte hostkey[], int size)
			throws IOException;

	private OnionBaseSocket(InputStream in, OutputStream out, byte hostkey[], int size) throws IOException
	{
		initiateOnionConnection(in, out, hostkey, size);

	}

	public OnionBaseSocket(Socket sock, byte hostkey[], int size) throws IOException
	{
		this(sock.getInputStream(), sock.getOutputStream(), hostkey, size);
	}

	public OnionBaseSocket(OnionBaseSocket sock, byte hostkey[], int size) throws IOException
	{
		this(sock.getInputStream(), sock.getOutputStream(), hostkey, size);
	}

	public InputStream getInputStream()
	{
		return null;
	}

	public OutputStream getOutputStream()
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
