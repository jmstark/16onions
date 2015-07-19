/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sandbox;

import network.ConnectionListener;
import network.Server;

/**
 *
 * @author Emertat
 */
public class DummyDHT implements Server{
    ConnectionListener r;
    
    public DummyDHT(int port) {
        r = new ConnectionListener(this); // Telling the connectionListener to call 'this' after receiving a message.
        r.startServer(port);
        System.out.println("dummyDHT server started... Listening on port: " + port);
    }
    
    @Override
    public void handleMessage(String message) {
        // first two bytes are SIZE
        int size = message.getBytes()[0] << 8 | message.getBytes()[1];
        
        System.out.println("dummyDHT speaking: received message is:\n"
                + message
                + "\nand two byte size is " + size );
        // next two bytes are TYPE
        // next 32 bytes are KEY
        // after that, dependds.
    }
}
