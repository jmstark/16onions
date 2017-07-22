package com.voidphone.api;

import java.net.InetSocketAddress;

import com.voidphone.general.Util;

import rps.api.RpsPeerMessage;

/**
 * Simple class representing a peer. Avoids unnecessary conversions 
 * between key objects and their byte[] representations one would get 
 * when using RpsPeerMessages only.
 * 
 */
public class OnionPeer {
	
	public final byte[] hostkey;
	public final InetSocketAddress address;
	
	public OnionPeer(InetSocketAddress address, byte[] hostkey)
	{
		this.address = address;
		this.hostkey = hostkey;
	}
	
	public OnionPeer(RpsPeerMessage rpsMsg)
	{
		this(rpsMsg.getAddress(),Util.getHostkeyBytes(rpsMsg.getHostkey()));
	}

}
