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
 * In this scenario we use DHT_PUT to save an item, then we use DHT_GET to see
 * if the item is actually saved by the DHT and is retrievable.
 *
 * @author Emertat
 */
public class Scenario3 implements Validation {

    String content, key;

    public Scenario3(Configuration conf) {
        MyRandom r = new MyRandom();
        content = r.randString(200);
        key = r.randString(Protocol.KEY_LENGTH);
        DummyDHT dht = new DummyDHT(conf);
        KXProxy kx = new KXProxy(this, conf);
        kx.sendDHT_PUT(content, key);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        kx.sendDHT_GET(key);
    }

    /**
     * we know for sure that the received message is a DHT_GET_REPLY message.
     * retrieve the content: protocol.getMessage()
     *
     * @param message
     * @return
     */
    @Override
    public boolean validate(String message) {
        if (Protocol.DHT_GET_REPLY_isValid(message)) { // Correct message received
            System.out.println(message.length());
            System.out.println(Protocol.get_DHT_REPLY_content(message).length());
            if (Protocol.get_DHT_REPLY_content(message).equals(content)
                    && Protocol.get_DHT_REPLY_key(message).equals(key)) {
                System.out.println("DHT HAS WORKED FINE.");
                return true; // the retrieved content is valid.
            }
        }
        System.out.println("DHT DID NOT RESPOND CORRECTLY.");
        return false;
    }
}
