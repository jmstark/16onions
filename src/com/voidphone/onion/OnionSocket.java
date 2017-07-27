package com.voidphone.onion;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.DatagramChannel;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import com.voidphone.general.General;
import com.voidphone.general.SizeLimitExceededException;

import protocol.Connection;
import protocol.DisconnectHandler;
import protocol.MessageHandler;
import protocol.MessageParserException;
import protocol.Protocol;
import protocol.ProtocolException;
import protocol.Protocol.MessageType;

public class OnionSocket {
	public final static int MAX_ONION_MESSAGE_SIZE = 2 * Short.MAX_VALUE + 2 * Short.BYTES;
	private AsynchronousSocketChannel controlChannel;
	private final ByteBuffer controlReadBuffer;
	private final ByteBuffer controlWriteBuffer;
	private final Multiplexer multiplexer;

	/**
	 * Initializes the buffers.
	 * 
	 * @throws IOException
	 *             if there is an I/O-error
	 */
	public OnionSocket(Multiplexer m, AsynchronousSocketChannel cch) throws IOException {
		controlReadBuffer = ByteBuffer.allocate(MAX_ONION_MESSAGE_SIZE);
		controlWriteBuffer = ByteBuffer.allocate(MAX_ONION_MESSAGE_SIZE);
		this.controlChannel = cch;
		this.multiplexer = m;
		controlChannel.read(controlReadBuffer, null, new ReadCompletionHandler());
	}

	public OnionMessage read(short id) {
		return multiplexer.getQueue(id).poll();
	}

	private class ReadCompletionHandler implements CompletionHandler<Integer, Void> {
		OnionMessage message;

		@Override
		public void completed(Integer result, Void none) {
			if (result <= 0) {
				// TODO: signal error
				controlChannel.close();
				return;
			}
			message = OnionMessage.parse(controlReadBuffer);
			while (message != null) {
				try {
					multiplexer.getQueue(message.getId()).offer(message);
				} catch (IllegalArgumentException e) {
					// TODO: handle new connection from known hop, e.g., handle(this, message)
				}
				message = OnionMessage.parse(controlReadBuffer);
			}
			controlChannel.read(controlReadBuffer, null, this);
		}

		@Override
		public void failed(Throwable ex, Void none) {
			disconnect();
		}
	}
}
