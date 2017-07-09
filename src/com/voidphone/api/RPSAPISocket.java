package com.voidphone.api;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class RPSAPISocket extends APISocket {
	public RPSAPISocket(InetSocketAddress addr) throws IOException {
		super(addr);
	}

	/**
	 * Sends a RPS QUERY message to the RPS module, waits for the answer and
	 * parses it.
	 * 
	 * @return a random peer, represented by a RPSPEER-object
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public RPSPEER RPSQUERY() throws IOException {
		short size;
		short type;
		short port;
		byte v;
		InetAddress addr;
		byte hostkey[];

		// send RPS QUERY
		dos.writeShort(4);
		dos.writeShort(MSG_TYPE_RPS_QUERY);
		dos.flush();

		// receive answer
		size = dis.readShort();
		if (size < 12) {
			throw new IOException("RPS module sends too small message!");
		}
		type = dis.readShort();
		if (type != MSG_TYPE_RPS_PEER) {
			throw new IOException("RPS module sends message of type " + type
					+ "!");
		}
		port = dis.readShort();
		// read first reserved byte
		dis.readByte();
		v = (byte) (dis.readByte() & 1);
		size -= 8;
		if (v == 0) {
			byte buffer[] = new byte[4];

			dis.readFully(buffer);
			addr = InetAddress.getByAddress(buffer);
			size -= 4;
		} else {
			byte buffer[] = new byte[16];

			dis.readFully(buffer);
			addr = InetAddress.getByAddress(buffer);
			size -= 16;
		}
		hostkey = new byte[size];
		dis.readFully(hostkey);
		return new RPSPEER(new InetSocketAddress(addr, port), hostkey);
	}

	public static class RPSPEER {
		private InetSocketAddress address;
		private byte hostkey[];

		public RPSPEER(InetSocketAddress address, byte[] hostkey) {
			this.address = address;
			this.hostkey = hostkey;
		}

		public InetSocketAddress getAddress() {
			return address;
		}

		public byte[] getHostkey() {
			return hostkey;
		}
	}
}
