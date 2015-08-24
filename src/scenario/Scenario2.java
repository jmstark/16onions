/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scenario;

import java.io.File;
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
 * in this scenario we test the DHT's ability to respond to a TRACE message.
 *
 * @author Emertat
 */
public class Scenario2 {

    public Scenario2(String confFile, ArrayList<Integer> ports) {
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
            DummyKX kx = new DummyKX(tempConfFile);
            MyRandom r = new MyRandom();
            kx.trace(r.randString(Protocol.KEY_LENGTH));
        } catch (Exception ex) {
            Logger.logEvent("Could not initiate scenario 2. Stack Trace:\n"
                    + ex.getMessage());
            System.exit(1);
        }
    }
}
