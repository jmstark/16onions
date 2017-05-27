package com.voidphone.onion;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.voidphone.api.APIOnionAuthSocket;

public class OnionSocket {
	private static APIOnionAuthSocket oasock = null;

	private OnionSocket(InputStream in, OutputStream out, byte hostkey[],
			int size) throws IOException {
		APIOnionAuthSocket.AUTHSESSIONHS1 hs1;
		byte buffer[] = new byte[size];

		hs1 = oasock.AUTHSESSIONSTART(new APIOnionAuthSocket.AUTHSESSIONSTART(
				hostkey));
		out.write(hs1.getPayload());
		in.read(buffer);
		oasock.AUTHSESSIONINCOMINGHS2(new APIOnionAuthSocket.AUTHSESSIONINCOMINGHS2(
				hs1.getSession(), buffer));
	}

	public OnionSocket(Socket sock, byte hostkey[], int size)
			throws IOException {
		this(sock.getInputStream(), sock.getOutputStream(), hostkey, size);
	}

	public OnionSocket(OnionSocket sock, byte hostkey[], int size)
			throws IOException {
		this(sock.getInputStream(), sock.getOutputStream(), hostkey, size);
	}

	public InputStream getInputStream() {
		return null;
	}

	public OutputStream getOutputStream() {
		return null;
	}

	public static void initAPIOnionAuthSocket(APIOnionAuthSocket oasock) {
		if (oasock == null) {
			OnionSocket.oasock = oasock;
		}
	}
}
