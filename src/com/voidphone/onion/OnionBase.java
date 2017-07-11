package com.voidphone.onion;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.voidphone.api.Config;
import com.voidphone.api.OnionAuthAPISocket;

public class OnionBase {
	public final static int MAGIC_SEQ_CONNECTION_START = 0x7af3bef1;
	public final static int VERSION = 1;

	protected Socket nextHopCSock;
	protected DataInputStream nextHopCDis;
	protected DataOutputStream nextHopCDos;
	protected Config config;

	protected OnionBase(Config c) {
		config = c;
	}
	
	protected void connect(InetSocketAddress addr,
			byte hostkey[]) throws IOException {
		OnionAuthAPISocket.AUTHSESSIONHS1 hs1;
		short size;
		byte buffer[];

		// create new Onion connection
		nextHopCSock = new Socket();
		nextHopCSock.connect(addr);
		nextHopCDis = new DataInputStream(nextHopCSock.getInputStream());
		nextHopCDos = new DataOutputStream(nextHopCSock.getOutputStream());

		nextHopCDos.writeInt(MAGIC_SEQ_CONNECTION_START);
		nextHopCDos.writeInt(VERSION);

		// get hs1 from onionAuth and send it to remote peer
		hs1 = config.getOnionAuthAPISocket().AUTHSESSIONSTART(
				new OnionAuthAPISocket.AUTHSESSIONSTART(hostkey));
		nextHopCDos.writeShort(hs1.getPayload().length);
		nextHopCDos.write(hs1.getPayload());

		// read incoming hs2 into buffer. We need to know the length of hs2
		size = nextHopCDis.readShort();
		buffer = new byte[size];
		nextHopCDis.readFully(buffer, 0, size);

		config.getOnionAuthAPISocket().AUTHSESSIONINCOMINGHS2(
				new OnionAuthAPISocket.AUTHSESSIONINCOMINGHS2(hs1.getSession(),
						buffer));
	}
}
