/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scenario;

import java.io.File;
import java.util.ArrayList;
import protocol.Configuration;
import static protocol.Configuration.DEV_MODE;
import protocol.HostKey;
import protocol.Protocol;
import proxy.DHTProxy;
import proxy.KXProxy;
import sandbox.DummyDHT;
import sandbox.DummyKX;
import sandbox.DummyVOIP;
import tools.Logger;
import tools.MyRandom;

/**
 * This is the scenario where we send large amounts of data over the built
 * tunnel and measure the bandwidth of the connection. make sure you set up
 * working KX and DHT modules when applying this scenario, or else its no use.
 *
 * @author Emertat
 */
public class Scenario5 {

    /**
     * size of the data (in bytes) to send over the tunnel to measure its
     * performance.
     */
    private static int length = 100000000; //10MB
    /**
     *
     * @param confFile
     * @param ports
     */
    public Scenario5(String confFile, ArrayList<Integer> ports) {
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
            String tunnelIPV4 = voip.buildTunnel(hk.getPsuedoIdentity(),
                    Configuration.kxHops, conf.getOutreachPort(), 
                    conf.getOutreachHostname(), random.randString(Protocol.IPV6_LENGTH),
                    Configuration.peerIdentity);
            String data = random.randString(length);
            long start = System.currentTimeMillis();
            voip.send(tunnelIPV4, conf.voip_external_port, data);
            long end = System.currentTimeMillis();
            Logger.logEvent("Scenario 5 successfully sent " + length + " bytes "
                    + "of data over Tunnel in " + (((double) (start - end)) / 1000) + " seconds.");
            voip.destroyTunnel(hk.getPsuedoIdentity());
        } catch (Exception ex) {
            Logger.logEvent("Could not initiate scenario 5. Stack Trace:\n"
                    + ex.getMessage());
            System.exit(1);
        }
    }
}
