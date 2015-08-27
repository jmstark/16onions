/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scenario;

import java.io.File;
import static java.lang.Thread.sleep;
import java.util.ArrayList;
import protocol.Configuration;
import static protocol.Configuration.DEV_MODE;
import protocol.Protocol;
import proxy.DHTProxy;
import sandbox.DummyDHT;
import sandbox.DummyKX;
import tools.Logger;
import tools.MyRandom;

/**
 * scenario1 tests if a DHT Module can listen to a message, properly, receive
 * the message and not crash or close the connection meanwhile.
 *
 * @author Emertat
 */
public class Scenario1 {

    public Scenario1(String confFile, ArrayList<Integer> ports) {
        try {
            Configuration conf = new Configuration(new File(confFile));
            int fakeDHTPort = ports.remove(0);
            int realDHTPort = conf.getDHTPort();
            if (DEV_MODE || Configuration.DHT_CMD == null) {
                new DummyDHT(confFile);
            } else {
                Runtime.getRuntime().exec(Configuration.DHT_CMD
                        + " " + confFile);
            }
            conf.setDHTPort(fakeDHTPort);
            conf.setDHTHost(Configuration.LOCAL_HOST);
            String tempConfFile = conf.store();
            new DHTProxy(tempConfFile, realDHTPort);

            MyRandom r = new MyRandom();
            DummyKX kx = new DummyKX(tempConfFile);
            sleep(1000);
            kx.put(r.randBytes(Protocol.KEY_LENGTH), r.randBytes(Protocol.MAX_VALID_CONTENT));
        } catch (Exception ex) {
            Logger.logEvent("Could not initiate scenario 1. Stack Trace:\n"
                    + ex.getMessage());
            System.exit(1);

        }
    }
}
