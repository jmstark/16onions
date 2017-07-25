package com.voidphone.api;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.interfaces.RSAPublicKey;

import com.voidphone.general.General;
import com.voidphone.general.Util;
import com.voidphone.onion.Main;
import com.voidphone.onion.OnionBaseSocket;
import com.voidphone.onion.OnionConnectingSocket;

import onion.api.OnionApiMessage;
import onion.api.OnionCoverMessage;
import onion.api.OnionErrorMessage;
import onion.api.OnionTunnelBuildMessage;
import onion.api.OnionTunnelDataMessage;
import onion.api.OnionTunnelDestroyMessage;
import onion.api.OnionTunnelIncomingMessage;
import onion.api.OnionTunnelReadyMessage;
import protocol.MessageHandler;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;
import protocol.ProtocolException;
import protocol.Protocol.MessageType;

public class OnionApiSocket extends ApiSocket implements Main.Attachable {
	private OnionMessageHandler handler;
	protected DataInputStream dis;
	protected DataOutputStream dos;
	protected Config config;
	protected OnionConnectingSocket currentTunnel;
	protected OnionConnectingSocket nextTunnel;
	protected InetSocketAddress tunnelDestination;
	protected byte[] destinationHostkey;
	public static final int ipAddressLength = 4;
	
	
	public static final short MSG_TYPE_ONION_TUNNEL_BUILD = 560;
	public static final short MSG_TYPE_ONION_TUNNEL_READY = 561;
	public static final short MSG_TYPE_ONION_TUNNEL_INCOMING = 562;
	public static final short MSG_TYPE_ONION_TUNNEL_DESTROY = 563;
	public static final short MSG_TYPE_ONION_TUNNEL_DATA = 564;
	public static final short MSG_TYPE_ONION_ERROR = 565;
	public static final short MSG_TYPE_ONION_COVER = 566;

	public OnionApiSocket(SocketChannel sock, Config config) throws IOException {
		super(sock);
		handler = new OnionMessageHandler(null);
	}

	public void ONIONERROR(OnionErrorMessage oem) throws IOException {
		oem.send(writeBuffer);
		writeBuffer.flip();
		channel.write(writeBuffer);
	}

	public void ONIONTUNNELINCOMING(OnionTunnelIncomingMessage otim) throws IOException {
		otim.send(writeBuffer);
		writeBuffer.flip();
		channel.write(writeBuffer);
	}

	public void ONIONTUNNELREADY(OnionTunnelReadyMessage otrm) throws IOException {
		otrm.send(writeBuffer);
		writeBuffer.flip();
		channel.write(writeBuffer);
	}

	public OnionErrorMessage newOnionErrorMessage(Protocol.MessageType requestType, long tunnelID) {
		return new OnionErrorMessage(requestType, tunnelID);
	}

	public OnionTunnelIncomingMessage newOnionTunnelIncomingMessage(long tunnelID, byte hostkey[]) {
		try {
			return new OnionTunnelIncomingMessage(tunnelID, hostkey);
		} catch (MessageSizeExceededException e) {
			General.fatalException(e);
			return null;
		}
	}

	public OnionTunnelReadyMessage newOnionTunnelReadyMessage(long tunnelID, byte hostkey[]) {
		try {
			return new OnionTunnelReadyMessage(tunnelID, hostkey);
		} catch (MessageSizeExceededException e) {
			General.fatalException(e);
			return null;
		}
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
		//register
		currentTunnel.registerChannel(Main.getSelector());
		oldTunnel.destroy();
		nextTunnel = null;
	}

	/**
	 * Forwards incoming tunnel data to the user interface
	 * @throws IOException 
	 * 
	 */
	public void forwardIncomingDataToUI(byte[] decryptedData, int tunnelID) throws Exception
	{
		ByteBuffer payloadBuffer = ByteBuffer.wrap(decryptedData);
		if(payloadBuffer.get() == OnionBaseSocket.MSG_DATA)
		{
			//extract payload according to size
			short size = payloadBuffer.getShort();
			byte[] sendToAPI = new byte[size];
			payloadBuffer.get(sendToAPI);
			
			//repack it according to API specs
			size += 8;
			payloadBuffer = ByteBuffer.allocate(size);
			payloadBuffer.putShort(size);
			payloadBuffer.putShort(OnionApiSocket.MSG_TYPE_ONION_TUNNEL_DATA);
			payloadBuffer.putInt(tunnelID);
			payloadBuffer.put(sendToAPI);
			dos.write(payloadBuffer.array());
		}
		
		//else, ignore it (cover traffic)
	}
	
	/**
	 * Listens for incoming TCP API connection, accepts API requests, unpacks them
	 * and calls the appropriate methods, and sends answers (if applicable). Needs
	 * to process ONION_TUNNEL_BUILD, ONION_TUNNEL_DESTROY, ONION_TUNNEL_DATA,
	 * ONION_COVER
	 * 
	 * @throws IOException
	 * @throws ProtocolException
	 * @throws MessageParserException
	 */
	public boolean handle() throws IOException, MessageParserException, ProtocolException {
		channel.read(readBuffer);
		handler.parseMessage(readBuffer);
		OnionApiMessage message = handler.getMessage();
		if (message instanceof OnionCoverMessage) {
			// TODO: do something
		} else if (message instanceof OnionTunnelBuildMessage) {
			// TODO: do something
			
			
			
		} else if (message instanceof OnionTunnelDataMessage) {
			// TODO: do something
		} else if (message instanceof OnionTunnelDestroyMessage) {
			// TODO: do something
		}
		return true;
	}

	private class OnionMessageHandler extends MessageHandler<Void> {
		private OnionApiMessage message;

		private OnionMessageHandler(Void closure) {
			super(closure);
		}

		@Override
		public void parseMessage(ByteBuffer buf, MessageType type, Void closure)
				throws MessageParserException, ProtocolException {
			switch (type) {
			case API_ONION_COVER:
				message = OnionCoverMessage.parse(buf);
				return;
			case API_ONION_TUNNEL_BUILD:
				message = OnionTunnelBuildMessage.parse(buf);
				try {

					// build the tunnel
					tunnelDestination = ((OnionTunnelBuildMessage) message).getAddress();
					RSAPublicKey key = ((OnionTunnelBuildMessage) message).getKey();
					currentTunnel = new OnionConnectingSocket(tunnelDestination,
							Util.getHostkeyBytes(((OnionTunnelBuildMessage) message).getKey()), config);

					// register
					currentTunnel.registerChannel(Main.getSelector());

					// reply ONION TUNNEL READY
					new OnionTunnelReadyMessage(currentTunnel.externalID, Util.getHostkeyBytes(key));
				} catch (Exception e) {
					e.printStackTrace();
				}
				return;
			case API_ONION_TUNNEL_DATA:
				message = OnionTunnelDataMessage.parse(buf);
				return;
			case API_ONION_TUNNEL_DESTROY:
				message = OnionTunnelDestroyMessage.parse(buf);
				
				int tunnelId = (int) ((OnionTunnelDestroyMessage) message).getId();
				
				//destroy main and backup tunnel
				if(currentTunnel.externalID == tunnelId)
				{
					try {
						currentTunnel.destroy();
						nextTunnel.destroy();
						tunnelDestination = null;
						currentTunnel = null;
						nextTunnel = null;
					}
					catch(Exception e)
					{
						//This could be normal, see TODO
						e.printStackTrace();
					}
				}
				// TODO: check for destruction request of OnionListenerSocket
			default:
				throw new ProtocolException("Unexpected message received");
			}
		}

		public OnionApiMessage getMessage() {
			return message;
		}
	}
}
