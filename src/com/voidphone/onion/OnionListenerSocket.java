/*
 * Copyright (c) 2017, Charlie Groh and Josef Stark. All rights reserved.
 * 
 * This file is part of 16onions.
 *
 * 16onions is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * 16onions is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with 16onions.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.voidphone.onion;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import com.voidphone.api.Config;
import com.voidphone.api.OnionAuthApiSocket;
import com.voidphone.general.General;
import com.voidphone.general.Util;

import auth.api.OnionAuthSessionHS2;
import auth.api.OnionAuthSessionIncomingHS1;

/**
 * Main application runs a TCP server socket. There, when it receives a new
 * connection, it then constructs an OnionServerSocket, passing the new TCP
 * socket to the constructor. Then it should send a TUNNEL_READY API message.
 * 
 */
public class OnionListenerSocket extends OnionBaseSocket {
	protected DataInputStream previousHopDis;
	protected DataOutputStream previousHopDos;
	protected DatagramChannel previousHopUdp;
	protected PreviousHopUdpHandler previousHopUdpHandler = new PreviousHopUdpHandler();
	protected DataInputStream nextHopDis;
	protected DataOutputStream nextHopDos;
	protected DatagramChannel nextHopUdp;
	protected NextHopUdpHandler nextHopUdpHandler = new NextHopUdpHandler();
	protected InetSocketAddress previousHopAddress;
	protected InetSocketAddress nextHopAddress;
	protected short previousHopMId;



	public OnionListenerSocket(InetSocketAddress previousHopAddress, Multiplexer m, short multiplexerId) throws IOException {
		super(m);
		previousHopMId = multiplexerId;
		this.config = Main.getConfig();
		/*previousHopDis = new DataInputStream(previousHopSocket.getInputStream());
		previousHopDos = new DataOutputStream(previousHopSocket.getOutputStream());
		// bind UDP socket to the same local and remote port as TCP
		previousHopUdp = DatagramChannel.open().bind(previousHopSocket.getLocalSocketAddress())
				.connect(previousHopSocket.getRemoteSocketAddress());
		previousHopUdp.register(Main.getSelector(), SelectionKey.OP_READ, previousHopUdpHandler);
		*/
		this.previousHopAddress = previousHopAddress;

		authApiId = Main.getOaas().register();
	}

	/**
	 * Encrypts payload for previous hop.
	 * 
	 * @return encrypted payload
	 * @throws Exception
	 */
	protected byte[] encrypt(byte[] payload) throws Exception {
		return super.encrypt(payload, 1);
	}

	/**
	 * Decrypts payload of previous hop.
	 * 
	 * @return Decrypted payload
	 * @throws Exception
	 */
	protected byte[] decrypt(byte[] payload) throws Exception {
		return super.decrypt(payload, 1);
	}

	/**
	 * Counterpart to authenticate() of OnionConnectingSocket. Since at the moment
	 * of authentication this node is the last one, we receive the authentication
	 * always unencrypted. The encryption was removed at the previous hop.
	 * 
	 * @return session ID
	 * 
	 * @throws IOException
	 */
	short authenticate() throws Exception {
		OnionAuthSessionHS2 hs2;

		OnionMessage incomingMsg = m.read(previousHopMId, previousHopAddress);
		
		ByteBuffer incomingDataBuf = ByteBuffer.wrap(incomingMsg.data);

		if (incomingDataBuf.getInt() != MAGIC_SEQ_CONNECTION_START | incomingDataBuf.getInt() != VERSION)
			throw new IOException("Tried to connect with non-onion node or wrong version node");

		// read incoming hostkey
		byte[] previousHopHostkey = new byte[incomingDataBuf.getInt()];
		incomingDataBuf.get(previousHopHostkey);

		// read incoming hs1
		byte[] hs1Payload = new byte[incomingDataBuf.getInt()];
		incomingDataBuf.get(hs1Payload);


		// get hs2 from onionAuth and send it back to remote peer
		authSessionIds[0] = apiRequestCounter++;
		hs2 = Main.getOaas().AUTHSESSIONINCOMINGHS1(new OnionAuthSessionIncomingHS1(authSessionIds[0],
				Util.getHostkeyObject(previousHopHostkey), hs1Payload));
		ByteArrayOutputStream outgoingDataBAOS = new ByteArrayOutputStream();
		DataOutputStream outgoingData = new DataOutputStream(outgoingDataBAOS);
		outgoingData.writeInt(hs2.getPayload().length);
		outgoingData.write(hs2.getPayload());
		
		m.writeControl(new OnionMessage(mId, previousHopAddress, outgoingDataBAOS.toByteArray()));

		return (short) hs2.getSessionID();
	}

	/**
	 * This function is used to build and destroy tunnels. Building/destroying is
	 * done iteratively. A control message only consists of message type, address
	 * length (4 or 6, depending on IP version), IP address and port number (same
	 * port number for TCP and UDP). The function receives the message, and if the
	 * next hop is already known the message gets forwarded there, if not, the next
	 * hop is constructed.
	 * 
	 * @throws IOException
	 */
	void processNextControlMessage() throws Exception {
		ctlDataBuf.clear();

		// When invoking this method, the authentication has already
		// succeeded, i.e. the data is encrypted irrespective of whether
		// we need to forward it or it is for us.

		byte[] encryptedData = new byte[previousHopDis.readShort()];
		previousHopDis.readFully(encryptedData);
		byte[] data = decrypt(encryptedData);

		if (nextHopDos != null) {
			// forward the data, now that we peeled off one layer of encryption.
			nextHopDos.writeShort(data.length);
			nextHopDos.write(data);
			nextHopDos.flush();
		}

		else {
			// the data is for us, we are (at least until now) the end of the tunnel
			// and the data has no more encryption layers, i.e. it is now plaintext.
			ctlDataBuf.put(data);
			short msgType = ctlDataBuf.getShort();
			if (msgType == MSG_BUILD_TUNNEL) {
				// A request to add a new hop and forward all data to that.
				// Unpack the hop address and then open the connection.
				byte[] rawAddress = new byte[ctlDataBuf.getShort()];
				ctlDataBuf.get(rawAddress);
				InetAddress nextHopAddress = InetAddress.getByAddress(rawAddress);
				short nextHopPort = ctlDataBuf.getShort();
				Socket nextHopSocket = new Socket(nextHopAddress, nextHopPort);
				nextHopDis = new DataInputStream(nextHopSocket.getInputStream());
				nextHopDos = new DataOutputStream(nextHopSocket.getOutputStream());

				// TODO: register also TCP channel of next hop,
				// because Heartbeat-messages may come from the next hop (all other control
				// messages go into the opposite direction, which is already bound in main()).
				// nextHopSocket.getChannel().register(selector, SelectionKey.OP_READ,
				// nextHopTcpHandler);

				// bind UDP socket to the same local and remote port as TCP
				nextHopUdp = DatagramChannel.open().bind(nextHopSocket.getLocalSocketAddress())
						.connect(nextHopSocket.getRemoteSocketAddress());
			} else if (msgType == MSG_DESTROY_TUNNEL) {
				// tear down the tunnel, i.e. connections to next and previous hop.
				if (nextHopDis != null)
					nextHopDis.close();
				if (nextHopDos != null)
					nextHopDos.close();
				if (nextHopUdp != null)
					nextHopUdp.close();
				if (previousHopDis != null)
					previousHopDis.close();
				if (previousHopDos != null)
					previousHopDos.close();
				if (previousHopUdp != null)
					previousHopUdp.close();
			}
		}
	}

	@Override
	public boolean handle() throws Exception {
		if (authSessionIds == null) {
			// not yet authenticated ->
			// the incoming data must be the handshake
			authSessionIds = new long[1];
			authSessionIds[0] = authenticate();
		} else {
			processNextControlMessage();
		}
		return false;
	}

	protected class PreviousHopUdpHandler implements Main.Attachable {

		@Override
		public boolean handle() throws Exception {

			// decrypt data
			byte[] payload = decryptAndUnpackNextUdpMessage(previousHopUdp);

			if (nextHopUdp == null) {
				// check if data is valid (non-cover), if so, send it to CM via API
				Main.getOas().newOnionTunnelIncomingMessage(externalID, payload);
			}

			else {
				// forward to next hop
				voipDataBuf.clear();
				voipDataBuf.putShort((short) payload.length);
				voipDataBuf.put(payload);
				nextHopUdp.write(voipDataBuf);
			}

			return false;
		}
	}

	protected class NextHopUdpHandler implements Main.Attachable {

		@Override
		public boolean handle() throws Exception {
			// get payload from next hop
			ByteBuffer size = ByteBuffer.allocate(2);
			nextHopUdp.read(size);
			byte[] payload = new byte[size.getShort()];
			ByteBuffer payloadBuffer = ByteBuffer.wrap(payload);
			nextHopUdp.read(payloadBuffer);

			// encrypt and send it to previous hop
			encryptAndPackAndSendUdpMessage(payload, previousHopUdp);

			return false;
		}

	}
}
