/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package protocol.gossip;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Objects;
import protocol.MessageSizeExceededException;
import protocol.MessageParserException;
import protocol.Protocol;

/**
 *
 * @author totakura
 */
public class GossipMessage extends protocol.Message {

    private final LinkedList<byte[]> pages;

    public GossipMessage() {
        this.addHeader(Protocol.MessageType.GOSSIP);
        pages = new LinkedList();
    }

    @Override
    public void send(ByteBuffer out) {
        super.send(out);
        for (byte[] page : pages) {
            out.putShort((short) page.length);
            out.put(page);
        }
    }

    public void addPage(byte[] content) throws MessageSizeExceededException {
        if ((this.size
                + content.length
                + 2 /*size of the page size field*/)
                > Protocol.MAX_MESSAGE_SIZE) {
            throw new MessageSizeExceededException();
        }
        pages.add(content);
        this.size += content.length + 2;
    }

    static public GossipMessage parse(final ByteBuffer buf)
            throws MessageParserException {
        short page_size;
        byte[] page;
        GossipMessage message;
        MessageParserException parser_exp;

        message = new GossipMessage();
        parser_exp = new MessageParserException("Invalid Gossip message");
        while (buf.hasRemaining()) {
            page_size = buf.getShort();
            if (page_size > (Protocol.MAX_MESSAGE_SIZE
                    - Protocol.HEADER_LENGTH
                    - (2 * message.pages.size()))) {
                throw parser_exp;
            }
            if (buf.remaining() < page_size) {
                throw parser_exp;
            }
            page = new byte[page_size];
            buf.get(page);
            try {
                message.addPage(page);
            } catch (MessageSizeExceededException exp) {
                throw parser_exp;
            }
        }
        return message;
    }

    @Override
    public boolean equals(Object message) {
        if (!(message instanceof GossipMessage)) {
            return false;
        }
        if (!super.equals(message)) {
            return false;
        }
        GossipMessage other = (GossipMessage) message;
        if (!Arrays.deepEquals(this.pages.toArray(), other.pages.toArray())) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + Objects.hashCode(this.pages) + super.hashCode();
        return hash;
    }
}
