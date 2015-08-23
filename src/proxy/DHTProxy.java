/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package proxy;

import tools.ConnectionListener;
import tools.Server;
import protocol.Configuration;

/**
 *
 * @author Emertat
 */
public class DHTProxy implements Server {
    ConnectionListener cl;
    Configuration conf;
    public DHTProxy(Configuration conf){
        this.conf = conf;
        // initiate two servers. one impersonating KX , one impersonating DHT.
        cl = new ConnectionListener(this);
        cl.startServer(conf.getDHTPort()); // impersonating DHT.
        cl.startServer(conf.getKXPort()); // impersonating KX.
    }   
    @Override
    public void handleMessage(String message, int port) {
        if(port == conf.getDHTPort()){
        } else if(port == conf.getKXPort()){
        } 
    }
}