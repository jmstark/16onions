package com.voidphone.api;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import protocol.Connection;
import protocol.DisconnectHandler;
import protocol.Message;
import protocol.MessageHandler;
import protocol.MessageParserException;
import protocol.Protocol;
import protocol.ProtocolException;
import protocol.StreamTokenizer;

/**
 * partly copied from Connection in testing
 */
public class APISocket {
	private final SocketChannel channel;
	private final ByteBuffer readBuffer;
	private final ByteBuffer writeBuffer;

	public APISocket(InetSocketAddress addr) throws IOException {
		channel = SocketChannel.open();
		// TODO: set timeout
		channel.connect(addr);
		readBuffer = ByteBuffer.allocate(Protocol.MAX_MESSAGE_SIZE);
		writeBuffer = ByteBuffer.allocate(Protocol.MAX_MESSAGE_SIZE);
	}

	public final SocketChannel getChannel() {
		return channel;
	}

	public synchronized <OUT extends Message, IN extends Message> IN sendrecv(OUT out) {
		out.send(writeBuffer);
		writeBuffer.flip();
		channel.write(writeBuffer);
		channel.read(readBuffer);
		IN in = in.parse(readBuffer);
		return in;
	}
	
	public synchronized <OUT extends Message> void send(OUT out) {
		out.send(writeBuffer);
		writeBuffer.flip();
		channel.write(writeBuffer);
	}

	public void close() throws IOException {
		channel.close();
	}
}
