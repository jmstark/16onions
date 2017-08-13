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
import java.util.concurrent.TimeoutException;

import com.voidphone.general.AuthApiException;
import com.voidphone.general.General;
import com.voidphone.general.IllegalAddressException;
import com.voidphone.general.IllegalIDException;
import com.voidphone.general.OnionAuthErrorException;
import com.voidphone.general.SizeLimitExceededException;
import com.voidphone.general.TunnelCrashException;

import auth.api.OnionAuthCipherDecryptResp;
import auth.api.OnionAuthCipherEncryptResp;
import auth.api.OnionAuthSessionHS2;


/**
 * Class representing an incoming tunnel
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
	protected boolean forwardedHandshakeCompleted = false;

	
	/**
	 * Normal constructor.
	 * 
	 * @param previousHopAddress address of previous hop
	 * @param m the multiplexer
	 * @param multiplexerId multiplexer id of previous hop
	 * @throws SizeLimitExceededException
	 */
	public OnionListenerSocket(InetSocketAddress previousHopAddress, Multiplexer m, short multiplexerId) throws SizeLimitExceededException {
		super(m, Main.getOas().register());
		previousAndNextHopReadMId = multiplexerId;
		previousHopWriteMId = multiplexerId;
		this.previousHopAddress = previousHopAddress;
		authApiId = Main.getOaas().register();
		authSessionIds = new int[1];
	}

	/**
	 * Encrypts payload for previous hop.
	 * 
	 * @return encrypted payload
	 * @throws AuthApiException 
	 */
	protected byte[] encrypt(byte[] payload) throws AuthApiException {

		OnionAuthCipherEncryptResp response;
		try {
			response = Main.getOaas().AUTHCIPHERENCRYPT(Main.getOaas().
					newOnionAuthEncrypt(authApiId, authSessionIds[0],nextHopAddress != null && forwardedHandshakeCompleted,payload));
			if(nextHopAddress != null)
			{
				//Encryption will be enabled, as soon as the handshake is transferred
				forwardedHandshakeCompleted = true;
			}		
			return response.getPayload();	
		} catch (InterruptedException | IllegalIDException | OnionAuthErrorException e) {
			General.error("AUTH API communication error");
			throw new AuthApiException();
		}	
	}

	/**
	 * Decrypts payload of previous hop.
	 * 
	 * @return Decrypted payload
	 * @throws AuthApiException 
	 */
	protected byte[] decrypt(byte[] payload) throws AuthApiException  {
		OnionAuthCipherDecryptResp response;
		try {
			response = Main.getOaas().AUTHCIPHERDECRYPT(Main.getOaas().newOnionAuthDecrypt(authApiId, authSessionIds[0], payload));
		} catch (InterruptedException | IllegalIDException | OnionAuthErrorException e) {
			General.error("AUTH API communication error");
			throw new AuthApiException();
		}
		return response.getPayload();
	}

	/**
	 * Counterpart to authenticate() of OnionConnectingSocket. Since at the moment
	 * of authentication this node is the last one, we receive the authentication
	 * always unencrypted. The encryption was removed at the previous hop.
	 * 
	 * @return session ID
	 * @throws TunnelCrashException 
	 * 
	 */
	int authenticate() throws TunnelCrashException {
		
		try {
			OnionAuthSessionHS2 hs2;

			OnionMessage incomingMsg = m.read(previousHopWriteMId, previousHopAddress);
			
			ByteBuffer incomingDataBuf = ByteBuffer.wrap(incomingMsg.data);

			if (incomingDataBuf.getInt() != MAGIC_SEQ_CONNECTION_START | incomingDataBuf.getInt() != VERSION)
			{
				General.error("Tried to connect with incoming non-onion node or wrong version node");
				throw new TunnelCrashException();
			}

			// read incoming hs1
			byte[] hs1Payload = new byte[incomingDataBuf.getInt()];
			incomingDataBuf.get(hs1Payload);
			
			
			// get hs2 from onionAuth and send it back to remote peer
			hs2 = Main.getOaas().AUTHSESSIONINCOMINGHS1(Main.getOaas().newOnionAuthSessionIncomingHS1(authApiId, hs1Payload));
			authSessionIds[0] = hs2.getSessionID();
			
			ByteArrayOutputStream outgoingDataBAOS = new ByteArrayOutputStream();
			DataOutputStream outgoingData = new DataOutputStream(outgoingDataBAOS);
			outgoingData.write(hs2.getPayload());

			m.write(new OnionMessage(previousHopWriteMId, OnionMessage.CONTROL_MESSAGE, previousHopAddress, outgoingDataBAOS.toByteArray()));
			General.info("Authentication with previous hop successful");
			
			return hs2.getSessionID();
			
		} 
		
		catch (IllegalAddressException | InterruptedException | IOException | SizeLimitExceededException | IllegalIDException | OnionAuthErrorException e) {
			General.error("Couldn't construct incoming tunnel");
			General.fatalException(e);
			throw new TunnelCrashException();
		}


	}

	/**
	 * Fetches the next available message and processes and/or forwards it accordingly.
	 * 
	 * @return Only true, if the tunnel has been destroyed and cannot be reused
	 */
	public boolean getAndProcessNextMessage() {
		if(initiateDestruction)
		{
			destroy();
			return true;
		}
		try
		{
				
			//remove one layer of encryption.
			OnionMessage incomingMessage = m.read(previousHopWriteMId, previousHopAddress);
			
			//handle time-out
			if(incomingMessage == null)
			{
				if(heartbeatRetries>5)
				{
					throw new IOException("Tunnel timeout: no data received");				
				}
				if(nextHopAddress == null)
				{
					//send heartbeat, but not if we're an intermediate hop
					General.info("Tunnel still alive? Sending heartbeat, expecting some answer (e.g. cover traffic)");
					sendHeartbeat(previousHopWriteMId, previousHopAddress);
				}
				heartbeatRetries++;
				return false;
			}
	
			//No timeout, so we can reset the counter
			heartbeatRetries = 0;
			
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
					General.info("forwarding message to next hop");
				}
				else
				{
					payload = encrypt(incomingMessage.data);
					destinationAddress = previousHopAddress;
					destinationWriteMId = previousHopWriteMId;
					General.info("forwarding message to previous hop");
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
				if(payload[0] == MSG_HEARTBEAT)
				{
					//respond with some data so the other end knows we're alive
					General.info("Heartbeat received, responding with some data");
					sendCoverData(32);
					return false;
				}	
				
				if(payload[0] != MSG_DATA)
				{
					//ignore cover traffic
					General.info("Cover traffic received");
					return false;
				}
				
				General.info("MSG_DATA received: incoming data message");
				Main.getOas().ONIONTUNNELDATAINCOMING(Main.getOas().newOnionTunnelDataMessage(onionApiId, Arrays.copyOfRange(payload, 1, payload.length)));
				return false;
			}
			
			//At this point we know it is a control message for us
			ByteBuffer buffer = ByteBuffer.wrap(payload);
			byte messageType = buffer.get();
			
			if(messageType == MSG_DESTROY_TUNNEL)
			{
				General.info("MSG_DESTROY_TUNNEL received");
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
				General.info("MSG_BUILD_TUNNEL received");
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
				General.info("MSG_INCOMING_TUNNEL received");
				//Signal to our CM a new incoming tunnel
				Main.getOas().addActiveTunnel(this);
				Main.getOas().ONIONTUNNELINCOMING(Main.getOas().newOnionTunnelIncomingMessage(onionApiId));
			}
			return false;
			
		}
		
		catch(TunnelCrashException | IllegalAddressException | IllegalIDException | InterruptedException | SizeLimitExceededException | IOException | TimeoutException | AuthApiException e)
		{
			General.error("Tunnel broke, cannot re-use it");
			General.fatalException(e);
			return true;
		}

	}

	/**
	 * Tears the tunnel down.
	 */
	private void destroy(){
		try
		{
			//Close connection
			m.unregisterID(previousHopWriteMId, previousHopAddress);
			//Close auth and rps API connections
			Main.getOaas().unregister(authApiId);
		}
		catch(IllegalAddressException | IllegalIDException e)
		{
			
		}
	}
	
	@Override
	public void sendData(boolean isRealData, byte[] data) throws TunnelCrashException 
	{
		sendData(isRealData, data, previousHopWriteMId, previousHopAddress);
	}

}
