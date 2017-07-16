package com.voidphone.api;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class APISocket 
{
	public static final short MSG_TYPE_ONION_TUNNEL_BUILD = 560;
	public static final short MSG_TYPE_ONION_TUNNEL_READY = 561;
	public static final short MSG_TYPE_ONION_TUNNEL_INCOMING = 562;
	public static final short MSG_TYPE_ONION_TUNNEL_DESTROY = 563;
	public static final short MSG_TYPE_ONION_TUNNEL_DATA = 564;
	public static final short MSG_TYPE_ONION_ERROR = 565;
	public static final short MSG_TYPE_ONION_COVER = 566;
	
	public static final short MSG_TYPE_RPS_QUERY = 540;
	public static final short MSG_TYPE_RPS_PEER = 541;
	
	public static final short AUTH_SESSION_START = 600;
	public static final short AUTH_SESSION_HS1 = 601;
	public static final short AUTH_SESSION_INCOMING_HS1 = 602;
	public static final short AUTH_SESSION_HS2 = 603;
	public static final short AUTH_SESSION_INCOMING_HS2 = 604;
	public static final short AUTH_LAYER_ENCRYPT = 605;
	public static final short AUTH_LAYER_DECRYPT = 606;
	public static final short AUTH_LAYER_ENCRYPT_RESP = 607;
	public static final short AUTH_LAYER_DECRYPT_RESP = 608;
	public static final short AUTH_SESSION_CLOSE = 609;
	public static final short AUTH_ERROR = 610;



	private Socket sock;
	protected DataInputStream dis;
	protected DataOutputStream dos;

	public APISocket(InetSocketAddress addr) throws IOException {
		sock = new Socket();
		sock.connect(addr);
		dis = new DataInputStream(sock.getInputStream());
		dos = new DataOutputStream(sock.getOutputStream());
	}
	
	public void close() throws IOException {
		dis.close();
		dos.close();
		sock.close();
	}
}
