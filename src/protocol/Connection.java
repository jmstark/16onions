/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package protocol;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author totakura
 */
public class Connection<A> {
    private static final Logger logger = Logger.getLogger(Connection.class.getName());
    private final AsynchronousSocketChannel channel;
    private final ByteBuffer readBuffer;
    private final ByteBuffer writeBuffer;
    private final ReadCompletionHandler readCompletionHandler;
    private final WriteCompletionHandler writeCompletionHandler;
    private final StreamTokenizer tokenizer;
    private boolean writeQueued;
    private final LinkedList<Message> writeQueue;
    private MessageHandler<A,Boolean> topLevelHandler;

    public Connection(AsynchronousSocketChannel channel) {
        this.channel = channel;
        readBuffer = ByteBuffer.allocate(Protocol.MAX_MESSAGE_SIZE);
        writeBuffer = ByteBuffer.allocate(Protocol.MAX_MESSAGE_SIZE);
        this.writeQueued = false;
        this.writeQueue = new LinkedList();
        this.readCompletionHandler = new ReadCompletionHandler();
        this.writeCompletionHandler = new WriteCompletionHandler();
        this.tokenizer = new StreamTokenizer (new ClientMessageHandler());
    }

    public void receive(MessageHandler<A, Boolean> topLevelHandler) {
        this.topLevelHandler = topLevelHandler;
        channel.read(readBuffer, this, this.readCompletionHandler);
    }

    public void sendMsg(Message message) {
         writeQueue.add(message);
        if (writeQueued)
            return;
        message = writeQueue.remove();
        message.send(writeBuffer);
        writeBuffer.flip();
        writeQueued = true;
        channel.write(writeBuffer, this, writeCompletionHandler);
    }

    public void disconnect() {
        tokenizer.reset();
        writeQueue.clear();
        logger.fine("Disconnecting client");
        try {
            channel.close();
        } catch (IOException ex) {
            logger.warning(ex.toString());
        }
    }

    private class WriteCompletionHandler implements CompletionHandler<Integer, Connection> {
        @Override
        public void completed(Integer result, Connection connection) {
            if (result <= 0) {
                logger.warning("Write channel closed while trying to writing");
                disconnect();
            }
            if (writeBuffer.hasRemaining())
            {
                writeBuffer.compact();
                channel.write(writeBuffer, connection, writeCompletionHandler);
                return;
            }
            if (writeQueue.isEmpty())
                writeQueued = false;
            else
                channel.write(readBuffer, connection, writeCompletionHandler);
        }

        @Override
        public void failed(Throwable ex, Connection connection) {
            disconnect();
        }
    }

    /**
     * Simple message handler which logs each received message
     */
    private class ClientMessageHandler extends MessageHandler<Void, Void> {
        @Override
        public Void handleMessage (Message message, Void nothing) {
            boolean keepAlive;
            logger.log(Level.FINE, "Received message of type: {0}", message.getType().name());
            keepAlive = topLevelHandler.handleMessage(message);
            if (!keepAlive)
                disconnect();
            return null;
        }
    }

    private class ReadCompletionHandler implements CompletionHandler<Integer, Connection> {
        @Override
        public void completed(Integer result, Connection connection) {
            boolean waiting;

            if (result <= 0) {
                logger.warning("Failed to read");
                disconnect();
            }
            readBuffer.flip();
            try {
                waiting = tokenizer.input(readBuffer);
            } catch (ProtocolException ex) {
                logger.log(Level.SEVERE, "Protocol exception");
                disconnect();
                return;
            }
            readBuffer.flip();
            readBuffer.compact();
            channel.read(readBuffer, connection, readCompletionHandler);
        }

        @Override
        public void failed(Throwable ex, Connection connection) {
            disconnect();
        }
    }

    public static Connection create(SocketAddress socketAddress,
            AsynchronousChannelGroup channelGroup) throws IOException {
        AsynchronousSocketChannel channel = AsynchronousSocketChannel.open(channelGroup);
        channel.connect(socketAddress);
        return new Connection(channel);
    }

    public static Connection create(SocketAddress socketAddress) throws IOException {
        return create(socketAddress, null);
    }
}
