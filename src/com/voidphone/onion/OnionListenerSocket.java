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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

import com.voidphone.general.General;

import auth.api.OnionAuthCipherDecryptResp;
import auth.api.OnionAuthCipherEncryptResp;
import auth.api.OnionAuthSessionHS2;


/**
 * Main application runs a TCP server socket. There, when it receives a new
 * connection, it then constructs an OnionListenerSocket, passing the neccessary
 * connection info (multiplexer, address, mID) to the constructor. 
 * Then it should send a TUNNEL_READY API message.
 * 
 */
public class OnionListenerSocket extends OnionBaseSocket {
	protected InetSocketAddress previousHopAddress;
	protected InetSocketAddress nextHopAddress;
	
	/* Multiplexer IDs to read and write to/from previous/next hops.
	 * For reading from whichever hop has sent a message to us, 
	 * we need to merge both read IDs.
	 * This doesn't affect writes, as we know to whom we want to send.
	 * Because of this, we keep read and write IDs for clarity's sake
	 * separate, even though they actually might be the same.
	 */
	protected short previousAndNextHopReadMId;
	protected short previousHopWriteMId;
	protected short nextHopWriteMId;





	public OnionListenerSocket(InetSocketAddress previousHopAddress, Multiplexer m, short multiplexerId) throws Exception {
		super(m);
		previousAndNextHopReadMId = multiplexerId;
		previousHopWriteMId = multiplexerId;
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
		OnionAuthCipherEncryptResp response = 
				Main.getOaas().AUTHCIPHERENCRYPT(Main.getOaas().
						newOnionAuthEncrypt(authApiId, authSessionIds[0],nextHopAddress != null,payload));
		
		return response.getPayload();		
	}

	/**
	 * Decrypts payload of previous hop.
	 * 
	 * @return Decrypted payload
	 * @throws Exception
	 */
	protected byte[] decrypt(byte[] payload) throws Exception {
		OnionAuthCipherDecryptResp response = 
				Main.getOaas().AUTHCIPHERDECRYPT(Main.getOaas().newOnionAuthDecrypt(authApiId, authSessionIds[0], payload));
		return response.getPayload();
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
	int authenticate() throws Exception {
		OnionAuthSessionHS2 hs2;

		OnionMessage incomingMsg = m.read(previousHopWriteMId, previousHopAddress);
		
		ByteBuffer incomingDataBuf = ByteBuffer.wrap(unpadData(incomingMsg.data));

		if (incomingDataBuf.getInt() != MAGIC_SEQ_CONNECTION_START | incomingDataBuf.getInt() != VERSION)
			throw new IOException("Tried to connect with non-onion node or wrong version node");

		// read incoming hs1
		byte[] hs1Payload = new byte[incomingDataBuf.getInt()];
		incomingDataBuf.get(hs1Payload);
		
		
		// get hs2 from onionAuth and send it back to remote peer
		authSessionIds[0] = apiRequestCounter++;
		hs2 = Main.getOaas().AUTHSESSIONINCOMINGHS1(Main.getOaas().newOnionAuthSessionIncomingHS1(authSessionIds[0], hs1Payload));

		ByteArrayOutputStream outgoingDataBAOS = new ByteArrayOutputStream();
		DataOutputStream outgoingData = new DataOutputStream(outgoingDataBAOS);
		outgoingData.writeInt(hs2.getPayload().length);
		outgoingData.write(hs2.getPayload());
		
		m.write(new OnionMessage(previousHopWriteMId, OnionMessage.CONTROL_MESSAGE, previousHopAddress, outgoingDataBAOS.toByteArray()));

		General.info("Authentication with previous hop successfull");
		
		return hs2.getSessionID();
	}

	/**
	 * Fetches the next available message and processes and/or forwards it accordingly.
	 * 
	 * @return Only true, if the tunnel has been destroyed and cannot be reused
	 * @throws Exception
	 */
	boolean getAndProcessNextMessage() throws Exception {
		
		
		
		//remove one layer of encryption. TODO: maybe it's from nextHopAddress?
		OnionMessage incomingMessage = m.read(previousHopWriteMId, previousHopAddress);
		byte[] payload;

		
		//The message is not for us, so forward it
		if(nextHopAddress != null)
		{
			//Now we need to determine where it came from, so we can either de- or encrypt and then
			//forward it into the right direction.
			InetSocketAddress destinationAddress;
			short destinationWriteMId;
			
			if(incomingMessage.address.equals(previousHopAddress))
			{
				payload = decrypt(incomingMessage.data);
				destinationAddress = nextHopAddress;
				destinationWriteMId = nextHopWriteMId;
			}
			else
			{
				payload = encrypt(incomingMessage.data);
				destinationAddress = previousHopAddress;
				destinationWriteMId = previousHopWriteMId;
			}
			
			m.write(new OnionMessage(destinationWriteMId, incomingMessage.type, destinationAddress, payload));
			return false;
		}
				
		//At this point it's clear that the message is for us and came from previous hop,
		//thus we can decrypt and process it.
		
		payload = decrypt(incomingMessage.data);
		
		//In case it is a VOIP-data message
		if(incomingMessage.type == OnionMessage.DATA_MESSAGE)
		{
			if(payload[0] != MSG_DATA)
				//ignore cover traffic
				return false;
			
			Main.getOas().ONIONTUNNELDATAINCOMING(Main.getOas().newOnionTunnelDataMessage(externalID, Arrays.copyOfRange(payload, 1, payload.length)));
			return false;
		}
		
		//At this point we know it is a control message for us
		ByteBuffer buffer = ByteBuffer.wrap(payload);
		short messageType = buffer.getShort();
		
		if(messageType == MSG_DESTROY_TUNNEL)
		{
			if(nextHopAddress != null)
				m.unregisterID(nextHopWriteMId, nextHopAddress);
			m.unregisterID(previousHopWriteMId, previousHopAddress);
			//No need to unregister previousAndNextHopReadMId as it was the same value as one of the above.
			nextHopAddress = null;
			previousHopAddress = null;
			return true;
		}
		else if(messageType == MSG_BUILD_TUNNEL)
		{
			byte[] rawAddress = new byte[buffer.get()];
			buffer.get(rawAddress);
			int port = buffer.getInt();
			nextHopAddress = new InetSocketAddress(InetAddress.getByAddress(rawAddress),port);
			m.registerAddress(nextHopAddress);
			nextHopWriteMId = m.registerID(nextHopAddress);
			
			//Merge multiplexer reads for previous and next hops.
			//Writes are unaffected, so we'll use previousAndNextHopReadMId
			//for reading for clarity's sake, even though it is the same as one of the write IDs.
			m.merge(previousHopWriteMId, previousHopAddress, nextHopWriteMId, nextHopAddress);
			//From this point on, all reads with previousAndNextHopReadMId can come from both directions.
		}
		else if(messageType == MSG_INCOMING_TUNNEL)
		{
			//Signal to our CM a new incoming tunnel
			externalID = buffer.getInt();
			Main.getOas().ONIONTUNNELINCOMING(Main.getOas().newOnionTunnelIncomingMessage(externalID));
		}
		return false;

	}

}
