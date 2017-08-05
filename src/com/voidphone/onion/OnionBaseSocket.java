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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.Random;

import com.sun.xml.internal.bind.v2.model.core.MapPropertyInfo;
import com.voidphone.api.Config;

/**
 * Base class for OnionConnectingSocket and OnionListenerSocket
 * 
 */
public abstract class OnionBaseSocket
{

	public final static int MAGIC_SEQ_CONNECTION_START = 0x7af3bef1;
	public final static int VERSION = 1;
	protected final byte MSG_BUILD_TUNNEL = 0xb;
	protected final byte MSG_DESTROY_TUNNEL = 0xe;
	public final static byte MSG_DATA = 0xd;
	protected final byte MSG_COVER = 0xc;
	protected int[] authSessionIds;
	protected Config config;
	public final int externalID;
	protected static int idCounter = 1;
	protected int authApiId;
	protected Multiplexer m;

	
	protected static int apiRequestCounter = 1;
	
	
	public OnionBaseSocket(Multiplexer m)
	{
		this(m, idCounter++);
	}
	
	public OnionBaseSocket(Multiplexer m, int tunnelID)
	{
		this.m = m;
		externalID = tunnelID;
	}
	
	/**
	 * Pads data to the standard packet size and adds a size int at beginning
	 */
	protected byte[] padData(byte[] payload)
	{
		ByteBuffer output = ByteBuffer.allocate(Main.getConfig().onionSize);
		output.putInt(payload.length);
		output.put(payload);
		byte[] randomPadding = new byte[output.remaining()];
		new Random().nextBytes(randomPadding);
		output.put(randomPadding);
		return output.array();
	}
	
	/**
	 * Removes padding and size int and returns only the actual data
	 * @param paddedPayload
	 * @return the payload
	 */
	protected byte[] unpadData(byte[] paddedPayload)
	{
		ByteBuffer buffer = ByteBuffer.wrap(paddedPayload);
		int actualSize = buffer.getInt();
		byte[] payload = new byte[actualSize];
		buffer.get(payload);
		return payload;
	}
	
	
	/**
	 * Encrypts payload with the given number of layers, 0 layers = no encryption.
	 * 
	 * @param numLayers
	 * @return encrypted payload (or plaintext if numLayers == 0)
	 * @throws Exception 
	 */
	protected byte[] encrypt(byte[] payload, int numLayers) throws Exception
	{
		if(numLayers < 0)
			throw new Exception("Negative number of layers");
		
		if(numLayers == 0)
		{
			//unencrypted packets need to be padded and contain size as int
			return padData(payload);
		}
			
		// Make a byte array containing the sessionIds in reverse order,
		// so that each hop can "peel off" one layer
		DataOutputStream sessionIds = new DataOutputStream(new ByteArrayOutputStream());
		for(int i=0; i < numLayers; i++)
		{
			sessionIds.writeLong(authSessionIds[numLayers - i - 1]);
		}
		sessionIds.flush();
		
		//TODO: send the data to OnionAuth API and get encrypted data back.
		//Needed:
		//the byte[] or short[] inside sessionIds; payload

		return null;
	}
	
	/**
	 * Decrypts payload with the given number of layers, 0 layers = no decryption.
	 * 
	 * @param encryptedPayload
	 * @param numLayers number of encryption layers to remove.
	 * @return the decrypted payload.
	 * @throws Exception
	 */
	protected byte[] decrypt(byte[] encryptedPayload, int numLayers) throws Exception
	{
		if(numLayers < 0)
			throw new Exception("Negative number of layers");
		
		if(numLayers == 0)
		{
			//unencrypted packets are padded and contain size as int
			return unpadData(encryptedPayload);
		}
			
		// Make a byte array containing the sessionIds in reverse order,
		// because they were encrypted in that order and onionAuth expects
		// them like that
		DataOutputStream sessionIds = new DataOutputStream(new ByteArrayOutputStream());
		for(int i=0; i < numLayers; i++)
		{
			sessionIds.writeLong(authSessionIds[numLayers - i - 1]);
		}
		sessionIds.flush();
		
		//TODO: send the data to OnionAuth API and get decrypted data back.
		//Needed:
		//the byte[] or short[] inside sessionIds; payload

		return null;
			
	}
	
	protected abstract byte[] encrypt(byte[] payload) throws Exception;
	
	protected abstract byte[] decrypt(byte[] payload) throws Exception;
	
}
