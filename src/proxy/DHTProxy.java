/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package proxy;

import java.io.File;
import protocol.Configuration;

/**
 *
 * @author Emertat
 */
public class DHTProxy implements Runnable {

    Configuration conf;
    int realDHTPort;

    /**
     *
     * @param conf
     * @param realPort this is supposed to be the port that actual DHT module is
     * listening on. any messages received, should be forwarded to that port.
     */
    public DHTProxy(String confFile, int realDHTPort) {
        this.conf = new Configuration(new File(confFile));
        this.realDHTPort = realDHTPort;
        //startServer(conf.getDHTPort());
    }

    @Override
    public void run() {
    }
}
