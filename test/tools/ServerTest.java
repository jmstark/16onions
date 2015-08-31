/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import protocol.Message;
import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 *
 * @author troll
 */
public class ServerTest {

    private final Logger logger;    

    public ServerTest() {
        logger = Logger.getLogger(SelectorThreadTest.class.getName());
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {

    }

    @After
    public void tearDown() {
    }

    private class EchoServerHandler implements ServerHandler {

        @Override
        public boolean newConnectionHandler(ServerClient client) {
            logger.info("New client connected");
            return true;
        }

        @Override
        public boolean messageHandler(ServerClient client, ByteBuffer msg) {          
            System.out.print (new String(msg.array(), 0, msg.limit()));
            System.out.flush();
            client.writeMessage(msg);
            return true;
        }

        @Override
        public void disconnectHandler(ServerClient client) {
            logger.info("Client disconnected");
        }

    }

    @Test
    public void testSomeMethod() throws InterruptedException, IOException {        
        try {
            final Server server = new Server(new EchoServerHandler(), new InetSocketAddress(6001));
            server.start();
            Thread.sleep(1000000);
            server.stop();
        } catch (IOException ex) {
            Logger.getLogger(ServerTest.class.getName()).log(Level.SEVERE, null, ex);
            fail();
            return;
        }
    }

}
