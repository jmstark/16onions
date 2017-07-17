/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package protocol;

import java.nio.ByteBuffer;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import dht.api.DHTContent;
import dht.api.DHTKey;
import dht.api.DhtGetMessage;
import dht.api.DhtMessage;
import dht.api.DhtPutMessage;
import util.MyRandom;

/**
 *
 * @author totakura
 */
public class StreamTokenizerTest {
    boolean testResult;

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    private Object expResult;
    private final DhtGetMessage getMsg;
    private final DhtPutMessage putMsg;

    public StreamTokenizerTest() {
        byte[] key = MyRandom.randBytes(32);
        byte[] content = MyRandom.randBytes(128);
        putMsg = new DhtPutMessage(new DHTKey(key),
                (short) 3600, (byte) 3, new DHTContent(content));
        getMsg = new DhtGetMessage(new DHTKey(MyRandom.randBytes(32)));
    }

    @Before
    public void setUp() {
        testResult = false;
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of input method, of class StreamTokenizer.
     * @throws java.lang.Exception
     */
    @Test
    public void testInput() throws Exception {
        System.out.println("input");
        ByteBuffer buffer = ByteBuffer.allocate (1024);
        putMsg.send(buffer);
        getMsg.send(buffer);
        buffer.flip();
        StreamTokenizer instance = new StreamTokenizer(new MessageHandlerImpl(null));
        assertFalse("The tokenizer still expects input when complete messages are given",
                instance.input(buffer));
        assertTrue("The tokenizer did not move the position in the given buffer",
                   instance.input(buffer));
        assertTrue("Messages didn't match", testResult);
    }

    @Test
    public void testInputChunkedInput() throws Exception {
        StreamTokenizer instance = new StreamTokenizer(new MessageHandlerImpl(null));
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        putMsg.send(buffer);
        buffer.flip();
        assertFalse (instance.input(buffer));
        buffer.flip();
        buffer.compact();
        getMsg.send(buffer);
        buffer.flip();
        int limit = buffer.limit();
        buffer.limit(15);
        assertTrue(instance.input(buffer));
        assertTrue(instance.input(buffer));
        buffer.limit(limit);
        assertFalse(instance.input(buffer));
        assertTrue("Messages didn't match", testResult);
    }

    private class MessageHandlerImpl extends MessageHandler<Void> {
        int messagesReceived;

        public MessageHandlerImpl(Void closure) {
            super(closure);
        }

        @Override
        public void parseMessage(ByteBuffer buf,
                Protocol.MessageType type,
                Void closure) throws MessageParserException {
            DhtMessage message;
            boolean test = false;

            message = DhtMessage.parse(buf, type);
            if (0 == messagesReceived) {
                test = message.equals(putMsg);
            }
            if (1 == messagesReceived) {
                test = message.equals(getMsg);
            }
            if (!test) {
                return;
            }
            messagesReceived++;
            if (2 == messagesReceived) {
                testResult = true;
            }
        }
    }
}
