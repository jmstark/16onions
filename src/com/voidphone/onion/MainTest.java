package com.voidphone.onion;

import static org.junit.Assert.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.junit.Test;

public class MainTest {

	
	public static void main(String args[]) throws IOException {
		Socket bla = new Socket();
		bla.connect(new InetSocketAddress("127.0.0.1",30000));
		Socket foo = new Socket();
		foo.connect(new InetSocketAddress("127.0.0.1",30001));
		DataOutputStream dos = new DataOutputStream(foo.getOutputStream());
		DataInputStream dis = new DataInputStream(foo.getInputStream());
		dos.writeInt(0x7af3bef1);
		dos.writeInt(1);
		dos.writeShort(16);
		dos.write(new byte[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15});
		System.out.println(dis.readShort());
		System.out.println(dis.read());
		System.out.println(dis.read());
		System.out.println(dis.read());
		foo.getOutputStream().write(new byte[]{10,11,12});
		//bla.getOutputStream().write(new byte[]{1,2,3,4,5,6,7,8});
	}
}
