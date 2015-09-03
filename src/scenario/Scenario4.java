/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scenario;

import java.io.File;
import java.util.ArrayList;
import protocol.Configuration;
import static protocol.Configuration.DEV_MODE;
import proxy.DHTProxy;
import proxy.KXProxy;
import sandbox.DummyDHT;
import sandbox.DummyKX;
import sandbox.DummyVOIP;
import tools.Logger;

/**
 * This is a passive test. This is designed just to set up proxies between given
 * Modules, and Log The Events.
 *
 * @author Emertat
 */
public class Scenario4 {

    public Scenario4(String confFile, ArrayList<Integer> ports) {
        try {
            Configuration conf = new Configuration(new File(confFile));
            int fakeDHTPort = ports.remove(0);
            int realDHTPort = conf.getDHTPort();
            int fakeKXPort = ports.remove(0);
            int realKXPort = conf.getKXPort();
            String realDHTHost = conf.getDHTHost();
            String realKXHost = conf.getKXHost();
            /**
             * ********* INIT DHT *********
             */
            conf.setKXPort(fakeKXPort);
            conf.setKXHost(Configuration.LOCAL_HOST);
            conf.setDHTPort(realDHTPort);
            conf.setDHTHost(realDHTHost);
            String configForDHT = conf.store(); // create a new config file.
            if (DEV_MODE || Configuration.DHT_CMD == null) {
                DummyDHT.instantiate(conf); // initiate DHT with fake KX port, real DHT port.
            } else {
                Runtime.getRuntime().exec(Configuration.DHT_CMD
                        + " " + configForDHT);
            }

            /**
             * ********* INIT KX *********
             */
            conf.setKXPort(realKXPort);
            conf.setKXHost(realKXHost);
            conf.setDHTPort(fakeDHTPort);
            conf.setDHTHost(Configuration.LOCAL_HOST);
            String configForKX = conf.store(); // create a new config file.
            if (DEV_MODE || Configuration.KX_CMD == null) {
                DummyKX kx = new DummyKX(configForKX); // initiate the KX module with this config file.
            } else {
                Runtime.getRuntime().exec(Configuration.KX_CMD
                        + " " + configForKX);
            }
            /**
             * ********* INIT VOIP *********
             */
            conf.setKXPort(fakeKXPort);
            conf.setKXHost(Configuration.LOCAL_HOST);
            conf.setDHTPort(fakeDHTPort);
            conf.setDHTHost(Configuration.LOCAL_HOST);
            String configForVOIP = conf.store();
            if (DEV_MODE || Configuration.VOIP_CMD == null) {
                new DummyVOIP(configForVOIP); // initiate voip module with this config file.
            } else {
                Runtime.getRuntime().exec(Configuration.VOIP_CMD
                        + " " + configForVOIP);
            }
            /**
             * ********* INIT PROXY MODULES *********
             */
            conf.setKXPort(fakeKXPort);
            conf.setKXHost(realKXHost);
            conf.setDHTPort(fakeDHTPort);
            conf.setDHTHost(realDHTHost);
            String configForProxies = conf.store();
            new KXProxy(configForProxies, realKXPort);
            new DHTProxy(configForProxies, realDHTPort);
        } catch (Exception ex) {
            Logger.logEvent("Could not initiate scenario 4. Stack Trace:\n"
                    + ex.getMessage());
            System.exit(1);
        }
    }
}
