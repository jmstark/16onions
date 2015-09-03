/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scenario;

import java.io.File;
import java.nio.charset.StandardCharsets;
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
 * In this scenario we use DHT_PUT to save an item, then we use DHT_GET to see
 * if the item is actually saved by the DHT and is retrievable. The same
 * Scenario can easily be exploited to impose Performance and Stress tests on
 * DHT module! simply modify the scenario and set a loop on the kx.put() to
 * measure DHT's abilities.
 *
 * @author Emertat
 */
public class Scenario3 {

    byte[] content, key;

    public Scenario3(String confFile, ArrayList<Integer> ports) {
        try {
            Configuration conf = new Configuration(new File(confFile));
            if (DEV_MODE || Configuration.DHT_CMD == null) {
                DummyDHT.instantiate(conf);
            } else {
                Runtime.getRuntime().exec(Configuration.DHT_CMD
                        + " " + confFile);
            }
            int realDHTPort = conf.getDHTPort();
            conf.setDHTPort(ports.remove(0));
            conf.setDHTHost(Configuration.LOCAL_HOST);
            String fakeConfFile = conf.store();
            new DHTProxy(fakeConfFile, realDHTPort);
            DummyKX kx = new DummyKX(fakeConfFile);
            MyRandom r = new MyRandom();
            content = r.randBytes(200);
            key = r.randBytes(Protocol.KEY_LENGTH);
            kx.put(key, content);
            String reply = kx.get(new String(key, StandardCharsets.US_ASCII)); //for sure, its one or more DHT_GET_REPLY messages.
            String replies[] = Protocol.breakDHT_GET_REPLY(reply);
            boolean validity = false;
            for (String res : replies) {
                if (Protocol.DHT_GET_REPLY_isValid(res)
                        && Protocol.get_DHT_REPLY_content(res).equals(content)
                        && Protocol.get_DHT_REPLY_key(res).equals(key)) {
                    // at least one of the results is the value we saved.
                    validity = true;
                }
            }
            if (validity) {
                if (Configuration.LOG_ALL) {
                    Logger.logEvent("Scenario 3: Success: DHT module has saved the value as expected.");
                }
            } else {
                Logger.logEvent("DHT module Failed to retrieve content: '"
                        + content + "', under key: '" + key + "', " + replies.length
                        + " invalid messages received:\n" + reply);
            }
        } catch (Exception ex) {
            Logger.logEvent("Could not initiate scenario 3. Stack Trace:\n"
                    + ex.getMessage());
            System.exit(1);
        }
    }
}
