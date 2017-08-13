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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Random;

import com.voidphone.general.AuthApiException;
import com.voidphone.general.General;
import com.voidphone.general.IllegalAddressException;
import com.voidphone.general.SizeLimitExceededException;
import com.voidphone.general.TunnelCrashException;


/**
 * Base class for OnionConnectingSocket and OnionListenerSocket
 * 
 */
public abstract class OnionBaseSocket
{

	protected final int MAGIC_SEQ_CONNECTION_START = 0x7af3bef1;
	protected final int VERSION = 1;
	protected final byte MSG_BUILD_TUNNEL = 0xb;
	protected final byte MSG_INCOMING_TUNNEL = 0x1;
	protected final byte MSG_DESTROY_TUNNEL = 0xe;
	protected final byte MSG_DATA = 0xd;
	protected final byte MSG_COVER = 0xc;
	protected final byte MSG_HEARTBEAT = 0xa;
	protected int[] authSessionIds;
	protected int authApiId;
	protected int onionApiId;
	protected Multiplexer m;
	protected int heartbeatRetries = 0;

	public int getOnionApiId()
	{
		return onionApiId;
	}
	
	public OnionBaseSocket(Multiplexer m, int onionApiId) throws SizeLimitExceededException
	{
		this.m = m;
		authApiId = Main.getOaas().register();
		this.onionApiId = onionApiId;
	}
	
	/**
	 * Sends (real) VOIP-data
	 * @param data the payload
	 * @throws TunnelCrashException 
	 * @throws Exception
	 */
	public void sendRealData(byte[] data) throws TunnelCrashException  
	{
		sendData(true,data);
	}
	
	/**
	 * Sends fake/ cover traffic of the specified size
	 * @param size size of the cover traffic to generate
	 * @throws TunnelCrashException 
	 * @throws Exception
	 */
	public void sendCoverData(int size) throws TunnelCrashException
	{
		byte[] rndData = new byte[size];
		new Random().nextBytes(rndData);
		sendData(false, rndData);
	}
	
	/**
	 * Sends data through the tunnel, be it real VOIP or cover traffic.
	 * @param isRealData indicates if the data is real data or not (-> cover traffic)
	 * @param data the payload
	 * @throws TunnelCrashException 
	 * @throws Exception 
	 */
	public void sendData(boolean isRealData, byte[] data, short targetHopMId, InetSocketAddress targetHopAddress) throws TunnelCrashException
	{
		ByteBuffer payload = ByteBuffer.allocate(data.length + 1);
		payload.put(isRealData ? MSG_DATA : MSG_COVER);
		payload.put(data);
		
		try {
			m.write(new OnionMessage(targetHopMId, OnionMessage.DATA_MESSAGE, targetHopAddress, encrypt(payload.array())));
		} catch (IllegalAddressException | InterruptedException | IOException | SizeLimitExceededException
				| AuthApiException e) {
			General.error("Could not write to tunnel");
			throw new TunnelCrashException();
		}		
	}
	
	
	public void sendHeartbeat(short targetHopMId, InetSocketAddress targetHopAddress) throws TunnelCrashException {
		try {
			m.write(new OnionMessage(targetHopMId, OnionMessage.DATA_MESSAGE, targetHopAddress,
					encrypt(new byte[] { MSG_HEARTBEAT })));
		} catch (IllegalAddressException | InterruptedException | IOException | SizeLimitExceededException
				| AuthApiException e) {
			General.error("Could not write to tunnel");
			throw new TunnelCrashException();
		}
	}

	
	
	public abstract void sendData(boolean isRealData, byte[] data) throws TunnelCrashException;
	
	protected abstract byte[] encrypt(byte[] payload) throws AuthApiException;
	
	protected abstract byte[] decrypt(byte[] payload) throws AuthApiException;
	
}
