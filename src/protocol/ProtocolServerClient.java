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
 * @author troll
 */
public class ProtocolServerClient {

    private final AsynchronousSocketChannel channel;
    private final ByteBuffer readBuffer;
    private final ReadCompletionHandler readCompletionHandler;
    private final WriteCompletionHandler writeCompletionHandler;
    private static final Logger logger = Logger.getLogger(ProtocolServerClient.class.getName());
    private final StreamTokenizer tokenizer;
    private final ServerClientMessageHandler serverMessageHandler;
    private final ByteBuffer writeBuffer;
    private boolean writeQueued;
    private final LinkedList<Message> writeQueue;

    @SuppressWarnings("LeakingThisInConstructor")
    public ProtocolServerClient(AsynchronousSocketChannel channel,
            ServerClientMessageHandler handler) {
        this.channel = channel;
        readBuffer = ByteBuffer.allocate(Protocol.MAX_MESSAGE_SIZE);
        writeBuffer = ByteBuffer.allocate(Protocol.MAX_MESSAGE_SIZE);
        this.writeQueued = false;
        this.writeQueue = new LinkedList();
        this.readCompletionHandler = new ReadCompletionHandler();
        this.writeCompletionHandler = new WriteCompletionHandler();
        this.serverMessageHandler = handler;
        this.tokenizer = new StreamTokenizer (new ClientMessageHandler());
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

    private class WriteCompletionHandler implements CompletionHandler<Integer, ProtocolServerClient> {
        @Override
        public void completed(Integer result, ProtocolServerClient client) {
            if (result <= 0) {
                logger.warning("Write channel closed while trying to writing");
                disconnect();
            }
            if (writeBuffer.hasRemaining())
            {
                writeBuffer.compact();
                channel.write(writeBuffer, client, writeCompletionHandler);
                return;
            }
            if (writeQueue.isEmpty())
                writeQueued = false;
            else
                channel.write(readBuffer, client, writeCompletionHandler);
        }

        @Override
        public void failed(Throwable ex, ProtocolServerClient client) {
            disconnect();
        }
    }

    /**
     * Simple message handler which logs each received message
     */
    private class ClientMessageHandler implements MessageHandler {
        @Override
        public void handleMessage (Message message) {
            boolean keepAlive;
            logger.log(Level.FINE, "Received message of type: {0}", message.getType().name());
            keepAlive = serverMessageHandler.handleMessage(message, ProtocolServerClient.this);
            if (!keepAlive)
                disconnect();
        }
    }

    private class ReadCompletionHandler implements CompletionHandler<Integer, ProtocolServerClient> {
        @Override
        public void completed(Integer result, ProtocolServerClient client) {
            boolean waiting;

            if (result <= 0) {
                logger.warning("Failed to read");
                client.disconnect();
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
            client.channel.read(readBuffer, client, readCompletionHandler);
        }

        @Override
        public void failed(Throwable ex, ProtocolServerClient client) {
            client.disconnect();
        }
    }

    private void disconnect() {
        tokenizer.reset();
        writeQueue.clear();
        readBuffer.clear();
        writeBuffer.clear();
        try {
            logger.fine("Disconnecting client");
            channel.close();
        } catch (IOException ex) {
            logger.warning(ex.toString());
        }
    }

}
