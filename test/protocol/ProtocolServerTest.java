/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package protocol;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import protocol.dht.DHTContent;
import protocol.dht.DHTKey;
import protocol.dht.DhtGetMessage;
import protocol.dht.DhtMessage;
import protocol.dht.DhtPutMessage;
import tools.MyRandom;

/**
 *
 * @author totakura
 */
public class ProtocolServerTest {

    private AsynchronousChannelGroup channelGroup;
    private ProtocolEchoServer server;
    private int port;
    private final DhtGetMessage dhtGetMsg;
    private final DhtPutMessage dhtPutMsg;
    private DhtMessage compareMessage;
    private Connection client;

    private class ProtocolEchoServer extends ProtocolServer {

        public ProtocolEchoServer(SocketAddress socketAddress, AsynchronousChannelGroup channelGroup) throws IOException {
            super(socketAddress, channelGroup);
        }

        @Override
        protected boolean handleMessage(Message message, Connection connection) {
            assertEquals(compareMessage, message);
            connection.sendMsg(message);
            return true;
        }

    }

    public ProtocolServerTest() {
        channelGroup = null;
        try {
            channelGroup = AsynchronousChannelGroup.withFixedThreadPool(2, Executors.defaultThreadFactory());
        } catch (IOException e) {
            Assume.assumeNoException(e);
        }
        for (port = 6001, server = null;
                (port < 7000); port++) {
            try {
                server = new ProtocolEchoServer(new InetSocketAddress(port), channelGroup);
            } catch (IOException e) {
                continue;
            }
            if (null != server) {
                break;
            }
        }

        dhtGetMsg = new DhtGetMessage(new DHTKey(MyRandom.randBytes(32)));
        dhtPutMsg = new DhtPutMessage(dhtGetMsg.getKey(),
                (short) 3600, (byte) 3, new DHTContent(MyRandom.randBytes(1024)));
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        server.start();
        try {
            client = Connection.create(new InetSocketAddress(port), channelGroup);
        } catch (IOException ex) {
            Assume.assumeNoException(ex);
        }
    }

    @After
    public void tearDown() {
        try {
            server.stop();
        } catch (IOException ex) {
            Logger.getLogger(ProtocolServerTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        client.disconnect();
    }

    /**
     * Test of handleNewClient method, of class ProtocolServer.
     */
    @Test
    public void testProtocolServer() {
        System.out.println("testing ProtocolServer");
        compareMessage = dhtGetMsg;
        client.sendMsg(dhtGetMsg);
        client.receive(new ClientMessageHandler());
        try {
            assertTrue(channelGroup.awaitTermination(300, TimeUnit.SECONDS));
        } catch (InterruptedException ex) {
            Assume.assumeNoException(ex);
        }
        client.disconnect();
        assertTrue(success);
    }

    private boolean success = false;

    private class ClientMessageHandler extends MessageHandler<Void, Boolean> {

        @Override
        protected Boolean handleMessage(Message message, Void nothing) {
            assertEquals(compareMessage, message);
            success = true;
            try {
                server.stop();
            } catch (IOException ex) {
                Logger.getLogger(ProtocolServerTest.class.getName()).log(Level.SEVERE, null, ex);
            }
            return false; //closes the connection
        }
    }
}
