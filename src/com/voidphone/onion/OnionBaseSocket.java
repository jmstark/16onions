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
	
	
	protected abstract byte[] encrypt(byte[] payload) throws Exception;
	
	protected abstract byte[] decrypt(byte[] payload) throws Exception;
	
}
