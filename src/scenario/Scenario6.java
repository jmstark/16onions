/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scenario;

import java.io.File;
import java.util.ArrayList;
import protocol.Configuration;
import protocol.HostKey;
import protocol.Protocol;
import proxy.DHTProxy;
import proxy.KXProxy;
import sandbox.DummyKX;
import sandbox.DummyVOIP;
import tools.Logger;
import tools.MyRandom;

/**
 * In this scenario, that needs to take long time, availability of the system to
 * convey a call is tested over long terms. Make sure you set up working KX and
 * DHT modules when applying this scenario, or else its no use.
 *
 * @author Emertat
 */
public class Scenario6 {

    /**
     * the period to test the connection in milliseconds. for example if the
     * value is 6000, it means try connection every minute.
     */
    private int period = 6000;
    /**
     * number of times total, to test the connection. If the value is 60 * 24,
     * it means that the program will run for (60 * 24 * period) milliseconds to
     * respond.
     */
    private int totalNumber = 60 * 24;

    public Scenario6(String confFile, ArrayList<Integer> ports) {
        try {
            if (Configuration.DHT_CMD == null || Configuration.KX_CMD == null) {
                return;
            }
            Configuration conf = new Configuration(new File(confFile));
            MyRandom random = new MyRandom();
            HostKey hk = new HostKey(conf.getHostkey());
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
            Runtime.getRuntime().exec(Configuration.DHT_CMD + " " + configForDHT);
            /**
             * ********* INIT KX *********
             */
            conf.setKXPort(realKXPort);
            conf.setKXHost(realKXHost);
            conf.setDHTPort(fakeDHTPort);
            conf.setDHTHost(Configuration.LOCAL_HOST);
            String configForKX = conf.store(); // create a new config file.
            DummyKX kx = new DummyKX(configForKX); // initiate the KX module with this config file.
            Runtime.getRuntime().exec(Configuration.KX_CMD
                    + " " + configForKX);
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

            /**
             * ********* INIT VOIP *********
             */
            conf.setKXPort(fakeKXPort);
            conf.setKXHost(Configuration.LOCAL_HOST);
            conf.setDHTPort(fakeDHTPort);
            conf.setDHTHost(Configuration.LOCAL_HOST);
            String configForVOIP = conf.store();
            DummyVOIP voip = new DummyVOIP(configForVOIP); // initiate voip module with this config file.
            int successful = 0;
            for (int i = 0; i < totalNumber; i++) {
                String tunnelIPV4 = voip.buildTunnel(hk.getPsuedoIdentity(),
                        Configuration.kxHops, conf.getOutreachPort(),
                        conf.getOutreachHostname(), random.randString(Protocol.IPV6_LENGTH),
                        Configuration.peerIdentity);
                if (tunnelIPV4 != null) {
                    voip.destroyTunnel(hk.getPsuedoIdentity());
                    successful++;
                }
                Thread.sleep(period);
            }
            Logger.logEvent("Scenario 6 is done measuring. system is available "
                    + (100.0 * successful / totalNumber) + "% of the time.");
        } catch (Exception ex) {
            Logger.logEvent("Could not initiate scenario 5. Stack Trace:\n"
                    + ex.getMessage());
            System.exit(1);
        }

    }
}
