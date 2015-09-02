/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package protocol;

import java.nio.ByteBuffer;

/**
 *
 * @author troll
 */
public class StreamTokenizer {

    enum ParseState {
        SIZE,
        BODY
    }

    private ParseState state;
    private final MessageHandler handler;
    private int expect;

    public StreamTokenizer(MessageHandler handler) {
        this.reset();
        this.handler = handler;
    }

    public void reset() {
        this.state = ParseState.SIZE;
        this.expect = Protocol.SIZE_LENGTH;
    }

    public boolean input(ByteBuffer buf) throws ProtocolException {
        ByteBuffer tokenizedCopy;
        Message message;

        while (true) {
            if (buf.remaining() < this.expect) {
                return true;
            }
            switch (this.state) {
                case SIZE:
                    assert (Protocol.SIZE_LENGTH == this.expect);
                    buf.mark();
                    this.expect = buf.getShort();
                    buf.reset();
                    if (Protocol.MAX_MESSAGE_SIZE < this.expect) {
                        this.reset();
                        throw new ProtocolException("Protocol message is > 64KB");
                    }
                    this.state = ParseState.BODY;
                    continue;
                case BODY:
                    tokenizedCopy = buf.slice();
                    buf.position(buf.position() + this.expect);
                    message = Message.parseMessage(tokenizedCopy);
                    if (null == message) {
                        throw new ProtocolException("Bad protocol message given");
                    }
                    handler.handleMessage(message);
                    this.reset();
                    if (buf.remaining() == 0)
                        return false;
                    continue;
            }
        }
    }
}
