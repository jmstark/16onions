package com.voidphone.onion;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;

import com.voidphone.api.Config;
import com.voidphone.api.OnionAuthAPISocket;
import com.voidphone.api.OnionAuthAPISocket.AUTHSESSIONINCOMINGHS1;

public class OnionMiddle extends OnionBase implements Main.Attachable {
	private final static int UNAUTHENTICATED = 0;
	private final static int AUTHENTICATED = 1;

	private int state;
	private SocketChannel previousHopCSock;
	private DataInputStream previousHopCDis;
	private DataOutputStream previousHopCDos;

	public OnionMiddle(SocketChannel sock, Config c) {
		super(c);
		previousHopCSock = sock;
		previousHopCDis = new DataInputStream(
				Channels.newInputStream(previousHopCSock));
		previousHopCDos = new DataOutputStream(
				Channels.newOutputStream(previousHopCSock));
		state = UNAUTHENTICATED;
	}
	
	private int authenticate() throws IOException {
		OnionAuthAPISocket.AUTHSESSIONHS2 hs2;
		short size;
		byte buffer[];

		if (previousHopCDis.readInt() != MAGIC_SEQ_CONNECTION_START
				|| previousHopCDis.readInt() != VERSION)
			throw new IOException(
					"Tried to connect with non-onion node or wrong version node");

		// read incoming hs1 from remote peer into buffer
		size = previousHopCDis.readShort();
		buffer = new byte[size];
		previousHopCDis.readFully(buffer, 0, size);

		// get hs2 from onionAuth and send it back to remote peer
		hs2 = config.getOnionAuthAPISocket().AUTHSESSIONINCOMINGHS1(
				new AUTHSESSIONINCOMINGHS1(config.getHostkey(), buffer));
		previousHopCDos.writeShort(hs2.getPayload().length);
		previousHopCDos.write(hs2.getPayload());
		
		return AUTHENTICATED;
	}

	@Override
	public boolean handle() throws IOException {
		switch (state) {
		case UNAUTHENTICATED:
			state = authenticate();
			break;
		case AUTHENTICATED:
			byte type = previousHopCDis.readByte();
			// TODO: check type
			if (type == 0) {
				
			}
			break;
		default:

		}
		return false;
	}
}
