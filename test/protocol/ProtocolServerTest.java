/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package protocol;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Assume;
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
public class ProtocolServerTest {

    private AsynchronousChannelGroup channelGroup;
    private ProtocolEchoServer server;
    private int port;
    private final DhtGetMessage dhtGetMsg;
    private final DhtPutMessage dhtPutMsg;
    private DhtMessage compareMessage;
    private Connection client;

    private class ProtocolEchoServer extends ProtocolServer<Connection> {

        public ProtocolEchoServer(SocketAddress socketAddress,
                AsynchronousChannelGroup channelGroup) throws IOException {
            super(socketAddress, channelGroup);
        }

        @Override
        protected Connection handleNewClient(Connection connection) {
            connection.receive(new ServerMessageHandler(connection));
            return connection;
        }

        @Override
        protected void handleDisconnect(Connection closure) {
            //do nothing
        }

        private class ServerMessageHandler extends MessageHandler<Connection> {

            private int gets_received;
            private int puts_received;

            public ServerMessageHandler(Connection closure) {
                super(closure);
                this.gets_received = 0;
                this.puts_received = 0;
            }

            @Override
            public void parseMessage(ByteBuffer buf,
                    Protocol.MessageType type,
                    Connection connection) throws MessageParserException {
                switch (type) {
                    case DHT_GET:
                        gets_received++;
                        break;
                    case DHT_PUT:
                        puts_received++;
                        break;
                    case DHT_TRACE:
                    case DHT_GET_REPLY:
                    case DHT_TRACE_REPLY:
                        break;
                    default:
                        fail("unknown message received");
                }
                DhtMessage message = (DhtMessage) DhtMessage.parse(buf, type);
                assertEquals(compareMessage, message);
                connection.sendMsg(message);
            }

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
        Logger.getGlobal().setLevel(Level.INFO);
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        server.start();
        AsynchronousSocketChannel channel;
        channel = null;
        try {
            channel = AsynchronousSocketChannel.open(channelGroup);
        } catch (IOException ex) {
            Assume.assumeNoException(ex);
            throw new RuntimeException();
        }
        channel.connect(new InetSocketAddress(port));
        client = new Connection(channel, null);
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
        client.receive(new ClientMessageHandler(client));
        channelGroup.shutdown();
        try {
            assertTrue(channelGroup.awaitTermination(3, TimeUnit.SECONDS));
        } catch (InterruptedException ex) {
            Assume.assumeNoException(ex);
        }
        assertTrue(success);
        Logger.getGlobal().log(Level.INFO,
            "The above warning is expected as part of this test; the sky isn't falling");
    }

    private boolean success = false;

    private class ClientMessageHandler extends MessageHandler<Connection> {
        private int gets_received;
        private int puts_received;

        public ClientMessageHandler(Connection closure) {
            super(closure);
            gets_received = 0;
            puts_received = 0;
        }

        @Override
        public void parseMessage(ByteBuffer buf,
                Protocol.MessageType type,
                Connection connection) throws MessageParserException {
            switch (type) {
                case DHT_GET:
                    gets_received++;
                    break;
                case DHT_PUT:
                    puts_received++;
                    break;
                case DHT_TRACE:
                case DHT_GET_REPLY:
                case DHT_TRACE_REPLY:
                    break;
                default:
                    fail("unknown message received");
            }
            DhtMessage message = (DhtMessage) DhtMessage.parse(buf, type);
            assertEquals(compareMessage, message);
            if ((1 == gets_received) && (0 == puts_received)) {
                compareMessage = dhtPutMsg;
                connection.sendMsg(dhtPutMsg);
                return;
            }
            success = true;
            try {
                server.stop();
            } catch (IOException ex) {
                Logger.getLogger(ProtocolServerTest.class.getName()).log(Level.SEVERE, null, ex);
            }
            connection.disconnect();
        }
    }
}
