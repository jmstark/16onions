/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package protocol.gossip;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Assume;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;

/**
 *
 * @author totakura
 */
public class GossipMessageTest {

    public LinkedList<byte[]> pages;

    public GossipMessageTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        Random rnd = new Random();
        this.pages = new LinkedList();
        int n = rnd.nextInt(10) + 1;
        int size = 0;
        for (; n > 0; n--) {
            size = rnd.nextInt(200) + 1;
            byte[] content = new byte[size];
            rnd.nextBytes(content);
            this.pages.add(content);
        }
    }

    @After
    public void tearDown() {
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
        
    public GossipMessage create() throws MessageSizeExceededException {
        GossipMessage original = new GossipMessage();
        for (byte[] page : pages) {
            original.addPage(page);
        }
        return original;
    }
    
    @Test
    public void construct() throws MessageSizeExceededException {
        create();
    }

    @Test
    public void parse() throws MessageParserException {
        GossipMessage original, parsed_message;
        original = null;
        try {
            original = create();
        } catch (Exception ex) {
            Assume.assumeNoException(ex);
        }
        Assume.assumeNotNull(original);
        ByteBuffer buf = ByteBuffer.allocateDirect(protocol.Protocol.MAX_MESSAGE_SIZE);
        original.send(buf);
        buf.flip();
        parsed_message = (GossipMessage) protocol.Message.parseMessage(buf);
        Assert.assertTrue(original.equals(parsed_message));
    }
}
