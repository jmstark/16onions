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

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Random;

import com.voidphone.general.SizeLimitExceededException;


/**
 * Base class for OnionConnectingSocket and OnionListenerSocket
 * 
 */
public abstract class OnionBaseSocket
{

	protected final int MAGIC_SEQ_CONNECTION_START = 0x7af3bef1;
	protected final int VERSION = 1;
	protected final byte MSG_BUILD_TUNNEL = 0xb;
	protected final short MSG_INCOMING_TUNNEL = 0x1;
	protected final byte MSG_DESTROY_TUNNEL = 0xe;
	protected final byte MSG_DATA = 0xd;
	protected final byte MSG_COVER = 0xc;
	protected int[] authSessionIds;
	public int externalID;
	protected int authApiId;
	protected int onionApiId;
	protected Multiplexer m;

	public int getOnionApiId()
	{
		return onionApiId;
	}
	
	protected static int apiRequestCounter = 1;
	
	
	public OnionBaseSocket(Multiplexer m) throws SizeLimitExceededException
	{
		this(m, new Random().nextInt());
	}
	
	public OnionBaseSocket(Multiplexer m, int tunnelID) throws SizeLimitExceededException
	{
		this.m = m;
		externalID = tunnelID;
		authApiId = Main.getOaas().register();
		onionApiId = Main.getOas().register();
	}
	
	/**
	 * Sends (real) VOIP-data
	 * @param data the payload
	 * @throws Exception
	 */
	public void sendRealData(byte[] data) throws Exception
	{
		sendData(true,data);
	}
	
	/**
	 * Sends fake/ cover traffic of the specified size
	 * @param size size of the cover traffic to generate
	 * @throws Exception
	 */
	public void sendCoverData(int size) throws Exception
	{
		byte[] rndData = new byte[size];
		new Random().nextBytes(rndData);
		sendData(false, rndData);
	}
	
	/**
	 * Sends data through the tunnel, be it real VOIP or cover traffic.
	 * @param isRealData indicates if the data is real data or not (-> cover traffic)
	 * @param data the payload
	 * @throws Exception 
	 */
	public void sendData(boolean isRealData, byte[] data, short targetHopMId, InetSocketAddress targetHopAddress) throws Exception
	{
		ByteBuffer payload = ByteBuffer.allocate(data.length + 1);
		payload.put(MSG_DATA);
		payload.put(data);
		
		m.write(new OnionMessage(targetHopMId, OnionMessage.DATA_MESSAGE, targetHopAddress, encrypt(payload.array())));		
	}
	
	
	public abstract void sendData(boolean isRealData, byte[] data) throws Exception;
	
	protected abstract byte[] encrypt(byte[] payload) throws Exception;
	
	protected abstract byte[] decrypt(byte[] payload) throws Exception;
	
}
