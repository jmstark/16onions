/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package protocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author totakura
 */
public final class Connection {

    private static final Logger logger = Logger.getGlobal();
    private final AsynchronousSocketChannel channel;
    private final ByteBuffer readBuffer;
    private final ByteBuffer writeBuffer;
    private final ReadCompletionHandler readCompletionHandler;
    private final WriteCompletionHandler writeCompletionHandler;
    private final LinkedList<Message> writeQueue;
    private final DisconnectHandler disconnectHandler;
    private StreamTokenizer tokenizer;
    private boolean writeQueued;

    public Connection(AsynchronousSocketChannel channel,
            DisconnectHandler disconnectHandler) {
        this.channel = channel;
        readBuffer = ByteBuffer.allocate(Protocol.MAX_MESSAGE_SIZE);
        writeBuffer = ByteBuffer.allocate(Protocol.MAX_MESSAGE_SIZE);
        this.writeQueued = false;
        this.writeQueue = new LinkedList();
        this.readCompletionHandler = new ReadCompletionHandler();
        this.writeCompletionHandler = new WriteCompletionHandler();
        this.disconnectHandler = disconnectHandler;
    }

    public final AsynchronousSocketChannel getChannel() {
        return channel;
    }

    /**
     * Start to read from this connection for messages. Once the reading is
     * started, currently there is no way to stop it except from closing the
     * connection.
     * @param handler the message handler object
     */
    public void receive(MessageHandler handler) {
        this.tokenizer = new StreamTokenizer(handler);
        channel.read(readBuffer, this, this.readCompletionHandler);
    }

    public void sendMsg(Message message) {
        writeQueue.add(message);
        if (writeQueued) {
            return;
        }
        message = writeQueue.remove();
        message.send(writeBuffer);
        writeBuffer.flip();
        writeQueued = true;
        channel.write(writeBuffer, this, writeCompletionHandler);
    }

    public void disconnect() {
        if (null != this.disconnectHandler) {
            disconnectHandler.handleDisconnect();
        }
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
            if (writeBuffer.hasRemaining()) {
                channel.write(writeBuffer, connection, writeCompletionHandler);
                return;
            }
            writeBuffer.clear();
            if (writeQueue.isEmpty()) {
                writeQueued = false;
                return;
            }
            Message message = writeQueue.remove();
            message.send(writeBuffer);
            writeBuffer.flip();
            channel.write(writeBuffer, connection, writeCompletionHandler);
        }

        @Override
        public void failed(Throwable ex, Connection connection) {
            disconnect();
        }
    }

    private class ReadCompletionHandler implements CompletionHandler<Integer, Connection> {

        @Override
        public void completed(Integer result, Connection connection) {
            boolean waiting;

            if (result <= 0) {
                logger.warning("Failed to read");
                disconnect();
                return;
            }
            readBuffer.flip();
            try {
                waiting = tokenizer.input(readBuffer);
            } catch (ProtocolException | MessageParserException ex) {
                logger.log(Level.SEVERE, ex.toString());
                disconnect();
                return;
            }
            if (waiting) {
                //unflip the buffer so that we can use it for writing
                readBuffer.compact(); //move contents to beginning
                readBuffer.position(readBuffer.limit());
                readBuffer.limit(readBuffer.capacity());
            } else {
                readBuffer.clear();
            }
            channel.read(readBuffer, connection, readCompletionHandler);
        }

        @Override
        public void failed(Throwable ex, Connection connection) {
            disconnect();
        }
    }
}
