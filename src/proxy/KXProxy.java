/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package proxy;

import java.io.File;
import protocol.Configuration;

/**
 * This class impersonates a KX, to listen to messages that KX is supposed to
 * receive.
 *
 * @author Emertat
 */
public class KXProxy implements Runnable {

    Configuration conf;
    int realKXPort;

    public KXProxy(String confFile, int realKXPort) {
        this.conf = new Configuration(new File(confFile));
        this.realKXPort = realKXPort;
        //startServer(conf.getKXPort());
    }

    @Override
    public void run() {
    }
}
