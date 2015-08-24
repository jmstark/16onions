/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package proxy;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import tools.Server;
import protocol.Configuration;
import protocol.Protocol;
import tools.Logger;

/**
 *
 * @author Emertat
 */
public class DHTProxy extends Server {

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
        startServer(conf.getDHTPort());
    }

    @Override
    public void run() {
        try {
            Socket publicSocket = socketBuffer.remove(0);
            Socket dhtSocket = new Socket(conf.getDHTHost(), realDHTPort);
            publicSocket.setSoTimeout(2000);
            dhtSocket.setSoTimeout(2000);
            BufferedReader publicIn = new BufferedReader(new InputStreamReader(publicSocket.getInputStream()));
            BufferedReader dhtIn = new BufferedReader(new InputStreamReader(dhtSocket.getInputStream()));
            PrintWriter publicOut = new PrintWriter(publicSocket.getOutputStream());
            PrintWriter dhtOut = new PrintWriter(dhtSocket.getOutputStream());
            if (Configuration.LOG_ALL) {
                Logger.logEvent("A connection to DHT is established from port "
                        + publicSocket.getPort() + " On "
                        + publicSocket.getInetAddress().getHostName() + "("
                        + publicSocket.getInetAddress().getHostAddress() + ")");
            }
            while (true) {
                String messageToDHT = Protocol.read(publicIn);
                if (messageToDHT.length() == 0) { // public client has closed socket
                    if (Configuration.LOG_ALL) {
                        Logger.logEvent("Connection to the DHT is closed.");
                    }
                    break;
                }
                boolean validity = Protocol.sizeCheck(messageToDHT) > 0;
                if (!validity || Configuration.LOG_ALL) {
                    Logger.logEvent((validity ? "" : "IN") + "VALID MESSAGE TO DHT:\n" + messageToDHT);
                }
                dhtOut.write(messageToDHT);
                String messageFromDHT = Protocol.read(dhtIn);
                if (messageFromDHT.length() == 0) { // DHT has closd socket.
                    if (Configuration.LOG_ALL) {
                        Logger.logEvent("Connection is closed by DHT.");
                    }
                    break;
                }
                validity = Protocol.sizeCheck(messageToDHT) > 0;
                if (!validity || Configuration.LOG_ALL) {
                    Logger.logEvent((validity ? "" : "IN") + "VALID MESSAGE TO DHT:\n" + messageToDHT);
                }
                publicOut.write(messageFromDHT);
            }
        } catch (Exception ex) {
            Logger.logEvent("DHT Proxy reached failure. error details: "
                    + ex.getMessage());
        }
    }
}