/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scenario;

import java.io.File;
import java.util.ArrayList;
import protocol.Configuration;

/**
 * This is designed just to set up proxies.
 *
 * @author Emertat
 */
public class Scenario4 {

    public Scenario4(String confFile, ArrayList<Integer> ports) {
        Configuration conf = new Configuration(new File(confFile));
        int fakeDHTPort = ports.remove(0);
        int realDHTPort = conf.getDHTPort();
        int fakeKXPort = ports.remove(0);
        int realKXPort = conf.getKXPort();

        /**
         * ********* INIT DHT *********
         */
        conf.setKXPort(fakeKXPort);
        conf.setDHTPort(realDHTPort);
        String configForDHT = conf.store(); // create a new config file.
        // initiate DHT with fake KX port, real DHT port.

        /**
         * ********* INIT KX *********
         */
        conf.setKXPort(realKXPort);
        conf.setDHTPort(fakeDHTPort);
        String configForKX = conf.store(); // create a new config file.
        // initiate the KX module with this config file.
        /**
         * ********* INIT VOIP *********
         */
        conf.setKXPort(fakeKXPort);
        conf.setDHTPort(fakeDHTPort);
        String configForVOIP = conf.store();
        // initiate voip module with this config file.
        /**
         * ********* INIT TEST MODULES *********
         */
        conf.setKXPort(realKXPort);
        conf.setDHTPort(realDHTPort);        
    }
}
