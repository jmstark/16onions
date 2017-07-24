package com.voidphone.api;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;

import com.voidphone.onion.Main;
import com.voidphone.onion.OnionConnectingSocket;

public class OnionApiSocket implements Main.Attachable {
	protected DataInputStream dis;
	protected DataOutputStream dos;
	protected Config config;
	protected OnionConnectingSocket currentTunnel;
	protected OnionConnectingSocket nextTunnel;
	protected InetSocketAddress tunnelDestination;
	protected byte[] destinationHostkey;
	public static final int ipAddressLength = 4;
	

	public OnionApiSocket(SocketChannel sock, Config config) {
		dis = new DataInputStream(Channels.newInputStream(sock));
		dos = new DataOutputStream(Channels.newOutputStream(sock));
		this.config = config;
	}
	
	/**
	 * This function should be called shortly before a new round begins.
	 * It builds a second backup tunnel with the same end destination and external ID.
	 * 
	 * @throws Exception
	 */
	public void prepareNextTunnel() throws Exception
	{
		nextTunnel = new OnionConnectingSocket(tunnelDestination, destinationHostkey, config, currentTunnel.externalID);
	}
	
	/**
	 * This function should be called at the beginning of a new round.
	 * We switch over to the new tunnel and destroy the old one.
	 * @throws Exception 
	 */
	public void switchToNextTunnel() throws Exception
	{
		OnionConnectingSocket oldTunnel = currentTunnel;
		currentTunnel = nextTunnel;
		oldTunnel.destroy();
		nextTunnel = null;
	}

	/**
	 * Listens for incoming TCP API connection, accepts API requests, unpacks
	 * them and calls the appropriate methods, and sends answers (if
	 * applicable). Needs to process ONION_TUNNEL_BUILD, ONION_TUNNEL_DESTROY,
	 * ONION_TUNNEL_DATA, ONION_COVER
	 * @throws Exception 
	 * 
	 * @throws IOException
	 */
	public boolean handle(){
		try {

				short msgLength = dis.readShort();
				if (msgLength < 8) {
					throw new IOException("API message too short: " + msgLength
							+ "Bytes.");
				}
				short msgType = dis.readShort();
				switch (msgType) {
				case ApiSocket.MSG_TYPE_ONION_TUNNEL_BUILD:
					// extract all the data of the target node
					// skip reserved 2 bytes
					dis.readShort();
					int targetPort = dis.readShort();
					byte[] targetIpAddress = new byte[ipAddressLength];
					dis.readFully(targetIpAddress, 0, targetIpAddress.length);
					int hostkeyLength = msgLength
							- (8 + targetIpAddress.length);
					if (hostkeyLength <= 0)
						throw new IOException(
								"API message or target DER-hostkey too short");
					byte[] targetHostkey = new byte[hostkeyLength];
					dis.readFully(targetHostkey);
					
					//build the tunnel
					tunnelDestination = new InetSocketAddress(InetAddress.getByAddress(targetIpAddress), targetPort);
					currentTunnel = new OnionConnectingSocket(tunnelDestination, targetHostkey, config);
					
					// reply ONION TUNNEL READY
					dos.writeShort(8 + targetHostkey.length);
					dos.writeShort(ApiSocket.MSG_TYPE_ONION_TUNNEL_READY);
					dos.writeInt(currentTunnel.externalID);
					dos.write(targetHostkey);
					dos.flush();
					
					break;
				case ApiSocket.MSG_TYPE_ONION_TUNNEL_DESTROY:
					int tunnelId = dis.readInt();
					
					//destroy main and backup tunnel
					if(currentTunnel.externalID != tunnelId)
						throw new Exception("Tunnel ID doesn't match. "
								+ "Should our API support more than one traffic tunnel at the same time? ");
					currentTunnel.destroy();
					if(nextTunnel != null)
					{
						if(nextTunnel.externalID != tunnelId)
							throw new Exception("Tunnel ID doesn't match. "
								+ "Should our API support more than one traffic tunnel at the same time? ");
						nextTunnel.destroy();
					}
					
					tunnelDestination = null;
					currentTunnel = null;
					nextTunnel = null;
					break;
				case ApiSocket.MSG_TYPE_ONION_TUNNEL_DATA:
					tunnelId = dis.readInt();
					int dataLength = msgLength - 8;
					if (dataLength <= 0)
						throw new IOException("API message or data too short");
					byte[] data = new byte[dataLength];
					dis.readFully(data, 0, dataLength);
					// TODO: call function with the now unpacked arguments.
					break;
				case ApiSocket.MSG_TYPE_ONION_COVER:
					short coverSize = dis.readShort();
					// skip reserved 2 bytes
					dis.readShort();
					// TODO: call function with the now unpacked arguments.
					break;
				}


		} catch (Exception e) {
			System.out.println("API connection lost: " + e.getMessage());
		}
		return false;
	}
}
