/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scenario;

import protocol.Configuration;
import protocol.Protocol;
import proxy.KXProxy;
import sandbox.DummyDHT;
import tools.MyRandom;

/**
 * scenario1 tests if a DHT Module can listen to a message, properly, receive 
 * the message and not crash or close the connection meanwhile.
 *
 * @author Emertat
 */
public class Scenario1{

    public Scenario1(Configuration conf) {
        try {
            new DummyDHT(conf); //TODO: actually call the DHT module.
            Thread.sleep(1000);
            MyRandom r = new MyRandom();
            KXProxy kx = new KXProxy(null, conf);
            kx.sendDHT_PUT(r.randString(Protocol.MAX_VALID_CONTENT), r.randString(Protocol.KEY_LENGTH));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
