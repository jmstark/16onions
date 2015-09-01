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
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 *
 * @author troll
 */
public class ProtocolServerClient {

    private final AsynchronousSocketChannel channel;
    private final ByteBuffer buffer;
    private final ReadHandler readHandler;
    private final Logger logger;

    public ProtocolServerClient(AsynchronousSocketChannel channel) {
        this.logger = Logger.getGlobal(); //Logger.getLogger(ProtocolServerClient.class.getName());
        this.channel = channel;
        buffer = ByteBuffer.allocate(Protocol.MAX_MESSAGE_SIZE);
        this.readHandler = new ReadHandler();
        channel.read(buffer, this, this.readHandler);
    }

    static private class ReadHandler implements CompletionHandler<Integer, ProtocolServerClient> {

        private int toRead;
        private ParseState state;
        private final Logger logger;

        ReadHandler() {
            //we always expect to receive the size field of the header first
            this.toRead = Protocol.HEADER_LENGTH;
            this.state = ParseState.HEADER;
            this.logger = Logger.getGlobal();
        }

        @Override
        public void completed(Integer result, ProtocolServerClient client) {
            if (result <= 0) {
                logger.warning("Failed to read");
                client.disconnect();
            }
            switch(this.state) {
                case HEADER:

            }
        }

        @Override
        public void failed(Throwable ex, ProtocolServerClient client) {
            client.disconnect();
        }

        private enum ParseState {
            HEADER,
            BODY
        }
    }

    private void disconnect() {
        try {
            logger.fine("Disconnecting client");
            this.channel.close();
        } catch (IOException ex) {
            logger.warning(ex.toString());
        }
    }

}
