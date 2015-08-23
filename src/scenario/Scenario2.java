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
 * in this scenario we test the DHT's ability to respond to a TRACE message.
 *
 * @author Emertat
 */
public class Scenario2 implements Validation {

    public Scenario2(Configuration conf) {
        DummyDHT dht = new DummyDHT(conf);
        KXProxy kx = new KXProxy(this, conf);
        MyRandom r = new MyRandom();
        kx.sendDHT_TRACE(r.randString(Protocol.KEY_LENGTH));
    }

    @Override
    public boolean validate(String message) {
        System.out.println("dht trace reply with 5 hops has length: " + message.length());
        return true;
    }
}
