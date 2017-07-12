package com.voidphone.api;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;

import com.voidphone.onion.Main;

public class OnionAPISocket implements Main.Attachable {
	DataInputStream dis;
	public static final int ipAddressLength = 4;

	public OnionAPISocket(SocketChannel sock) {
		dis = new DataInputStream(Channels.newInputStream(sock));
	}

	/**
	 * Listens for incoming TCP API connection, accepts API requests, unpacks
	 * them and calls the appropriate methods, and sends answers (if
	 * applicable). Needs to process ONION_TUNNEL_BUILD, ONION_TUNNEL_DESTROY,
	 * ONION_TUNNEL_DATA, ONION_COVER
	 * 
	 * @throws IOException
	 */
	public boolean handle() {
		try {
			while (true) {
				short msgLength = dis.readShort();
				if (msgLength < 8) {
					throw new IOException("API message too short: " + msgLength
							+ "Bytes.");
				}
				short msgType = dis.readShort();
				switch (msgType) {
				case APISocket.MSG_TYPE_ONION_TUNNEL_BUILD:
					// skip reserved 2 bytes
					dis.readShort();
					short targetPort = dis.readShort();
					byte[] targetIpAddress = new byte[ipAddressLength];
					dis.readFully(targetIpAddress, 0, targetIpAddress.length);
					int hostkeyLength = msgLength
							- (8 + targetIpAddress.length);
					if (hostkeyLength <= 0)
						throw new IOException(
								"API message or target DER-hostkey too short");
					byte[] targetHostkey = new byte[hostkeyLength];
					dis.readFully(targetHostkey, 0, targetHostkey.length);

					//build the tunnel
					Main.constructTunnel(targetIpAddress, targetPort, targetHostkey, 2);
					
					//TODO: API response: tunnel built successfully
					break;
				case APISocket.MSG_TYPE_ONION_TUNNEL_DESTROY:
					int tunnelId = dis.readInt();
					// TODO: call function with the now unpacked arguments.
					break;
				case APISocket.MSG_TYPE_ONION_TUNNEL_DATA:
					tunnelId = dis.readInt();
					int dataLength = msgLength - 8;
					if (dataLength <= 0)
						throw new IOException("API message or data too short");
					byte[] data = new byte[dataLength];
					dis.readFully(data, 0, dataLength);
					// TODO: call function with the now unpacked arguments.
					break;
				case APISocket.MSG_TYPE_ONION_COVER:
					short coverSize = dis.readShort();
					// skip reserved 2 bytes
					dis.readShort();
					// TODO: call function with the now unpacked arguments.
					break;
				}

			}
		} catch (Exception e) {
			System.out.println("API connection lost: " + e.getMessage());
		}
		return false;
	}
}
