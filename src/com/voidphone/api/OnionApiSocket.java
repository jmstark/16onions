package com.voidphone.api;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import com.voidphone.general.General;
import com.voidphone.onion.Main;

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

	public OnionApiSocket(SocketChannel sock) throws IOException {
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
				return;
			case API_ONION_TUNNEL_DATA:
				message = OnionTunnelDataMessage.parse(buf);
				return;
			case API_ONION_TUNNEL_DESTROY:
				message = OnionTunnelDestroyMessage.parse(buf);
				return;
			default:
				throw new ProtocolException("Unexpected message received");
			}
		}

		public OnionApiMessage getMessage() {
			return message;
		}
	}
}
