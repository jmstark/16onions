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
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import com.voidphone.general.AuthApiException;
import com.voidphone.general.General;
import com.voidphone.general.IllegalAddressException;
import com.voidphone.general.IllegalIDException;
import com.voidphone.general.NoRpsPeerException;
import com.voidphone.general.OnionAuthErrorException;
import com.voidphone.general.SizeLimitExceededException;
import com.voidphone.general.TunnelCrashException;
import com.voidphone.general.Util;

import auth.api.OnionAuthDecryptResp;
import auth.api.OnionAuthEncryptResp;
import auth.api.OnionAuthSessionHS1;
import protocol.MessageSizeExceededException;
import rps.api.RpsPeerMessage;

/**
 * When the main application wants to build an outgoing tunnel, it creates an
 * instance of this class.
 */
public class OnionConnectingSocket extends OnionBaseSocket {
	private final byte destHostkey[];
	protected InetSocketAddress nextHopAddress;
	protected int rpsApiId;
	protected short nextHopMId;
	// Array holding all hops, including the tunnel end
	protected RpsPeerMessage hops[];
	OnionAuthSessionHS1 hs1;

	/**
	 * Construct a new OnionConnectingSocket to the specified target (destAddr and
	 * destHostkey) or a random node if destAddr or destHostkey are null. Use this
	 * constructor if you already have registered an onionApiId so that this tunnel
	 * can re-use it.
	 * 
	 * @param m
	 *            the multiplexer
	 * @param destAddr
	 *            target address
	 * @param destHostkey
	 *            target hostkey
	 * @param onionApiId
	 *            tunnel ID used for communication with onion ID
	 * @throws SizeLimitExceededException
	 * @throws TunnelCrashException
	 * @throws InterruptedException
	 * @throws NoRpsPeerException
	 */
	public OnionConnectingSocket(Multiplexer m, InetSocketAddress destAddr, byte[] destHostkey, int onionApiId)
			throws TunnelCrashException, SizeLimitExceededException, NoRpsPeerException, InterruptedException {
		super(m, onionApiId);

		this.destHostkey = destHostkey;
		int hopCount = Main.getConfig().hopCount;
		authSessionIds = new int[hopCount + 1];
		hops = new RpsPeerMessage[hopCount + 1];
		try {
			hops[hopCount] = new RpsPeerMessage(destAddr, Util.getHostkeyObject(destHostkey));
		} catch (MessageSizeExceededException e) {
			General.fatalException(e);
		}

		rpsApiId = Main.getRas().register();
		// Fill up an array with intermediate hops
		for (int i = 0; i < hopCount; i++) {
			addRpsHop(i);
		}
		for (int i = 0; i < 10; i++) {
			nextHopAddress = hops[0].getAddress();
			try {
				m.registerAddress(hops[0].getAddress());
			} catch (IOException | TimeoutException e) {
				General.warning("Couldn't connect to first hop - selecting another");
				addRpsHop(0);
				continue;
			}
			try {
				nextHopMId = m.registerID(hops[0].getAddress());
			} catch (IllegalAddressException e) {
				General.fatalException(e);
			}
			return;
		}
		try {
			Main.getRas().unregister(rpsApiId);
		} catch (IllegalIDException e) {
			General.fatalException(e);
		}
	}

	/**
	 * Actually builds the tunnel
	 * 
	 * @param newNextHopMId
	 *            Multiplexer ID of the next hop
	 * @param newNextHopAddress
	 *            Address of the next hop
	 * @throws TunnelCrashException
	 */
	public void constructTunnel(short newNextHopMId, InetSocketAddress newNextHopAddress) throws TunnelCrashException {
		nextHopMId = newNextHopMId;
		nextHopAddress = newNextHopAddress;
		constructTunnel();
	}

	/**
	 * Fills the given position with a random RPS hop
	 * 
	 * @param hopNum
	 *            position of the hop
	 * @throws NoRpsPeerException
	 * @throws InterruptedException
	 * @throws IllegalIDException
	 */
	void addRpsHop(int hopNum) throws NoRpsPeerException, InterruptedException {
		for (int retries = 0; retries < 10; retries++) {
			RpsPeerMessage msg;
			try {
				msg = Main.getRas().RPSQUERY(Main.getRas().newRpsQueryMessage(rpsApiId));
			} catch (IllegalIDException e) {
				General.fatalException(e);
				msg = null;
			}
			if (msg == null)
				throw new NoRpsPeerException();

			if (Arrays.asList(hops).contains(msg)) {
				continue;
			}

			hops[hopNum] = msg;
			return;
		}
		General.error("Not enough RPS peers");
		throw new NoRpsPeerException();
	}

	/**
	 * Actually builds the tunnel.
	 * 
	 * @throws TunnelCrashException
	 */
	public void constructTunnel() throws TunnelCrashException {
		try {
			authSessionIds[0] = finishAuthentication(Util.getHostkeyBytes(hops[0].getHostkey()), 0);
			General.info("Authenticated to hop 0, address: " + hops[0].getAddress());

			// Establish forwardings (if any)
			for (int i = 1; i < hops.length; i++) {
				// Send forwarding request to node i-1 ->
				// it connects to the next node i and then forwards there
				// everything it receives from that point on.

				// First, send the actual request along with the target address. Encrypted.
				ByteArrayOutputStream outgoingDataBAOS = new ByteArrayOutputStream();
				DataOutputStream outgoingData = new DataOutputStream(outgoingDataBAOS);

				outgoingData.write(MSG_BUILD_TUNNEL);
				byte[] rawAddress = hops[i].getAddress().getAddress().getAddress();
				outgoingData.write((byte) rawAddress.length);
				outgoingData.write(rawAddress);
				outgoingData.writeInt(hops[i].getAddress().getPort());
				byte[] encryptedPayload = encrypt(outgoingDataBAOS.toByteArray(), i);

				try {
					General.info("Sending tunnel building request to hop " + (i - 1) + ", address: "
							+ hops[i - 1].getAddress());
					m.write(new OnionMessage(nextHopMId, OnionMessage.CONTROL_MESSAGE, nextHopAddress,
							encryptedPayload));

					General.info("Sent tunnel building request to hop " + (i - 1) + ", address: "
							+ hops[i - 1].getAddress());

					// Now, we are indirectly connected to the target node.
					// Authenticate to that node.
					beginAuthentication(Util.getHostkeyBytes(hops[i].getHostkey()), i);

					authSessionIds[i] = finishAuthentication(Util.getHostkeyBytes(hops[i].getHostkey()), i);
					General.info("Authenticated to hop " + i + ", address: " + hops[i].getAddress());
				} catch (IOException | TunnelCrashException e) {
					General.warning("Extending tunnel failed; Select new hop and retry");
					addRpsHop(i);
					i--;
					continue;
				}
			}

			// Signal to the last hop, that it is the target so it can signal an incoming
			// tunnel to the CM
			ByteArrayOutputStream outgoingDataBAOS = new ByteArrayOutputStream();
			DataOutputStream outgoingData = new DataOutputStream(outgoingDataBAOS);
			outgoingData.writeByte(MSG_INCOMING_TUNNEL);
			outgoingData.writeInt(onionApiId);
			m.write(new OnionMessage(nextHopMId, OnionMessage.CONTROL_MESSAGE, nextHopAddress,
					encrypt(outgoingDataBAOS.toByteArray())));

			General.info("Signaled tunnel end to last hop");

			Main.getOas().addActiveTunnel(this);
			// Inform our CM, that the requested tunnel is ready.
			Main.getOas().ONIONTUNNELREADY(Main.getOas().newOnionTunnelReadyMessage(onionApiId, destHostkey));

			General.info("Signalled successful tunnel build to CM");
		} catch (SizeLimitExceededException | IllegalAddressException | InterruptedException | IOException
				| AuthApiException | IllegalIDException | NoRpsPeerException e) {
			General.error("Couldn't construct incoming tunnel");
			General.fatalException(e);
			throw new TunnelCrashException();
		}
	}

	public short getNextHopMId() {
		return nextHopMId;
	}

	/**
	 * Used after calling beginAuthentication so that another thread can take over
	 * once the auth response comes in. Use this to prevent blocking.
	 * 
	 * @return multiplexer ID of next hop
	 * @throws TunnelCrashException
	 */
	public short detachId() throws TunnelCrashException {
		try {
			m.detachID(nextHopMId, nextHopAddress);
			return nextHopMId;
		} catch (IllegalAddressException | IllegalIDException e) {
			General.error("Couldn't construct tunnel");
			throw new TunnelCrashException();
		}
	}

	/**
	 * Encrypts payload for end hop.
	 * 
	 * @param payload
	 *            the plaintext payload
	 * @return encrypted payload
	 * @throws AuthApiException
	 */
	protected byte[] encrypt(byte[] payload) throws AuthApiException {
		return encrypt(payload, authSessionIds.length);
	}

	/**
	 * Decrypts payload of end hop.
	 * 
	 * @param payload
	 *            the encrypted payload
	 * @return Decrypted payload
	 * @throws AuthApiException
	 */
	protected byte[] decrypt(byte[] payload) throws AuthApiException {
		return decrypt(payload, authSessionIds.length);
	}

	/**
	 * Encrypts payload with the given number of layers, 0 layers = no encryption.
	 * This method is only used during authentication phase, after that only
	 * encrypt(byte[]) is used.
	 * 
	 * @param payload
	 *            the payload which will be encrypted
	 * @param numLayers
	 *            number of encryption layers to apply. 0 = no encryption.
	 * @return encrypted payload (or plaintext if numLayers == 0)
	 * @throws AuthApiException
	 */
	protected byte[] encrypt(byte[] payload, int numLayers) throws AuthApiException {
		if (numLayers == 0) {
			// unencrypted packet
			return payload;
		}

		// Make an array containing the sessionIds in reverse order,
		// so that each hop can "peel off" one layer
		int[] neededSessionIds = new int[numLayers];
		for (int i = 0; i < numLayers; i++) {
			neededSessionIds[i] = authSessionIds[numLayers - 1 - i];
		}

		// send the data to OnionAuth API and get encrypted data back.
		OnionAuthEncryptResp response;
		try {
			response = Main.getOaas()
					.AUTHLAYERENCRYPT(Main.getOaas().newOnionAuthEncrypt(authApiId, neededSessionIds, payload));
		} catch (InterruptedException | IllegalIDException | OnionAuthErrorException e) {
			General.error("AUTH API communication error");
			throw new AuthApiException();
		}

		return response.getPayload();
	}

	/**
	 * Decrypts payload with the given number of layers, 0 layers = no decryption.
	 * This method is only used during authentication phase, after that only
	 * decrypt(byte[]) is used.
	 * 
	 * @param encryptedPayload
	 * @param numLayers
	 *            number of encryption layers to remove. 0 = no decryption.
	 * @return the decrypted payload.
	 * @throws AuthApiException
	 */
	protected byte[] decrypt(byte[] encryptedPayload, int numLayers) throws AuthApiException {
		if (numLayers == 0) {
			// unencrypted packet
			return encryptedPayload;
		}

		// Make an array containing the sessionIds in reverse order,
		// because they were encrypted in that order and onionAuth expects
		// them like that
		int[] neededSessionIds = new int[numLayers];
		for (int i = 0; i < numLayers; i++) {
			neededSessionIds[i] = authSessionIds[numLayers - 1 - i];
		}

		// send the data to OnionAuth API and get decrypted data back.
		OnionAuthDecryptResp response;
		try {
			response = Main.getOaas().AUTHLAYERDECRYPT(
					Main.getOaas().newOnionAuthDecrypt(authApiId, neededSessionIds, encryptedPayload));
		} catch (InterruptedException | IllegalIDException | OnionAuthErrorException e) {
			General.error("AUTH API communication error");
			throw new AuthApiException();
		}

		return response.getPayload();
	}

	/**
	 * Sends magic numbers and HS1. After that, the socket can be detached. Then,
	 * construct() and/or finishAuthentication() can be used after re-attaching to
	 * finish the authentication and tunnel building
	 * 
	 * @param hopHostkey
	 * @param numLayers
	 * @throws TunnelCrashException
	 */
	public void beginAuthentication(byte[] hopHostkey, int numLayers)
			throws TunnelCrashException, InterruptedException {

		try {
			if (hopHostkey == null)
				hopHostkey = Util.getHostkeyBytes(hops[0].getHostkey());
			ByteArrayOutputStream outgoingDataBAOS = new ByteArrayOutputStream();
			DataOutputStream outGoingData = new DataOutputStream(outgoingDataBAOS);

			outGoingData.writeInt(MAGIC_SEQ_CONNECTION_START);
			outGoingData.writeInt(VERSION);

			// get hs1 from onionAuth and send it to remote peer

			hs1 = Main.getOaas().AUTHSESSIONSTART(
					Main.getOaas().newOnionAuthSessionStartMessage(authApiId, Util.getHostkeyObject(hopHostkey)));
			outGoingData.writeInt(hs1.getPayload().length);
			outGoingData.write(hs1.getPayload());

			byte[] encryptedPayload = encrypt(outgoingDataBAOS.toByteArray(), numLayers);
			m.write(new OnionMessage(nextHopMId, OnionMessage.CONTROL_MESSAGE, nextHopAddress, encryptedPayload));
		} catch (IOException | IllegalIDException | OnionAuthErrorException | AuthApiException | IllegalAddressException
				| SizeLimitExceededException e) {
			General.error("Tunnel construction failed");
			throw new TunnelCrashException();
		}
	}

	/**
	 * Authenticates via OnionAuth. Encrypts with numLayers (0 = no encryption).
	 * 
	 * @param hopHostkey
	 *            the hostkey of the hop with which we want to authenticate
	 * @param numLayers
	 *            the number of layers needed for that hop
	 * @return session ID for OnionAuth API - necessary for de-/encryption
	 * @throws TunnelCrashException
	 */
	public int finishAuthentication(byte[] hopHostkey, int numLayers) throws TunnelCrashException {

		try {
			// read incoming hs2 into buffer. We need to know the length of hs2
			OnionMessage incomingMessage;
			incomingMessage = m.read(nextHopMId, nextHopAddress);
			byte[] hs2payload = decrypt(incomingMessage.data, numLayers);

			Main.getOaas().AUTHSESSIONINCOMINGHS2(
					Main.getOaas().newOnionAuthSessionIncomingHS2(authApiId, hs1.getSessionID(), hs2payload));

			return hs1.getSessionID();
		} catch (IllegalAddressException | IllegalIDException | InterruptedException | AuthApiException
				| TimeoutException e) {
			General.error("Tunnel construction failed");
			throw new TunnelCrashException();
		}

	}

	/**
	 * Sends data through the tunnel, be it real VOIP or cover traffic.
	 * 
	 * @param isRealData
	 *            indicates if the data is real data or not (-> cover traffic)
	 * @param data
	 *            the payload
	 * @throws TunnelCrashException
	 */
	@Override
	public void sendData(boolean isRealData, byte[] data) throws TunnelCrashException {
		sendData(isRealData, data, nextHopMId, nextHopAddress);
	}

	/**
	 * Sends a request to destroy the tunnel to all hops and tears the tunnel down.
	 */
	private void destroy() {
		try {
			byte[] plainMsg = new byte[] { MSG_DESTROY_TUNNEL };
			// send the message iteratively to all hops,
			// starting at the farthest one
			for (int i = authSessionIds.length; i > 0; i--) {
				byte[] encryptedMsg = encrypt(plainMsg, i);
				m.write(new OnionMessage(nextHopMId, OnionMessage.CONTROL_MESSAGE, nextHopAddress, encryptedMsg));
			}
			// Close connection
			m.unregisterID(nextHopMId, nextHopAddress);
			// Close auth and rps API connections
			Main.getOaas().unregister(authApiId);
			Main.getRas().unregister(rpsApiId);
		} catch (AuthApiException | IllegalAddressException | InterruptedException | IOException
				| SizeLimitExceededException | IllegalIDException e) {

		}
	}

	/**
	 * Gets the next UDP-message and processes it accordingly, depending on wheter
	 * it's real or cover traffic.
	 * 
	 * @throws InterruptedException
	 * @throws TunnelCrashException
	 * 
	 * @throws Exception
	 */
	public boolean getAndProcessNextMessage() throws InterruptedException, TunnelCrashException {
		if (initiateDestruction) {
			destroy();
			return true;
		}

		OnionMessage incomingMessage = null;
		try {
			incomingMessage = m.read(nextHopMId, nextHopAddress);
		} catch (TimeoutException e) {
			// Handle timeouts
			if (heartbeatRetries > 5) {
				General.error("Tunnel timeout - retries exceeded");
				throw new TunnelCrashException();
			}
			// send heartbeat
			General.info("Tunnel still alive? Sending heartbeat, expecting some answer (e.g. cover traffic)");
			sendHeartbeat(nextHopMId, nextHopAddress);

			heartbeatRetries++;
			return false;
		} catch (IllegalAddressException | IllegalIDException e) {
			General.fatalException(e);
		}

		// No timeout, so we can reset the counter
		heartbeatRetries = 0;

		// decrypt data
		byte[] payload;
		try {
			payload = decrypt(incomingMessage.data);
		} catch (AuthApiException e) {
			General.warning("Decryption failed!");
			return false;
		}

		if (payload[0] == MSG_HEARTBEAT) {
			// respond with some data so the other end knows we're alive
			General.info("Heartbeat received, responding with some data");
			sendCoverData(32);
			return false;
		}

		if (payload[0] != MSG_DATA) {
			// ignore cover traffic
			General.info("Cover traffic received");
			return false;
		}

		try {
			Main.getOas().ONIONTUNNELDATAINCOMING(Main.getOas().newOnionTunnelDataMessage(onionApiId,
					Arrays.copyOfRange(payload, 1, payload.length)));
		} catch (IllegalIDException e) {
			General.fatalException(e);
		}

		return false;
	}

}
