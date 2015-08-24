/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package proxy;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import tools.Server;
import protocol.Configuration;
import protocol.Protocol;
import tools.Logger;
import tools.MyRandom;

/**
 * This class impersonates a KX, to listen to messages that KX is supposed to
 * receive.
 *
 * @author Emertat
 */
public class KXProxy extends Server {

    Configuration conf;
    MyRandom r;
    int realKXPort;

    public KXProxy(String confFile, int realKXPort) {
        this.conf = new Configuration(new File(confFile));
        this.realKXPort = realKXPort;
        startServer(conf.getKXPort());
        r = new MyRandom();
    }

    @Override
    public void run() {
        try {
            Socket publicSocket = socketBuffer.remove(0);
            Socket kxSocket = new Socket(conf.getKXHost(), realKXPort);
            publicSocket.setSoTimeout(2000);
            kxSocket.setSoTimeout(2000);
            BufferedReader publicIn = new BufferedReader(new InputStreamReader(publicSocket.getInputStream()));
            BufferedReader kxIn = new BufferedReader(new InputStreamReader(kxSocket.getInputStream()));
            PrintWriter publicOut = new PrintWriter(publicSocket.getOutputStream());
            PrintWriter kxOut = new PrintWriter(kxSocket.getOutputStream());
            if (Configuration.LOG_ALL) {
                Logger.logEvent("A connection to KX is established from port "
                        + publicSocket.getPort() + " On "
                        + publicSocket.getInetAddress().getHostName() + "("
                        + publicSocket.getInetAddress().getHostAddress() + ")");
            }
            while (true) {
                try {
                    String messageToKX = Protocol.read(publicIn);
                    if (messageToKX.length() == 0) { // public has closed socket
                        if (Configuration.LOG_ALL) {
                            Logger.logEvent("Connection to the KX is closed.");
                        }
                        break;
                    }
                    boolean validity = Protocol.sizeCheck(messageToKX) > 0;
                    if (!validity || Configuration.LOG_ALL) {
                        Logger.logEvent((validity ? "" : "IN")
                                + "VALID MESSAGE TO KX:\n" + messageToKX);
                    }
                    // forward it to the KX Module. don't close any of two connections.
                    kxOut.write(messageToKX);
                    String messageFromKX = Protocol.read(kxIn);
                    if (messageFromKX.length() == 0) { // KX has closd socket.
                        if (Configuration.LOG_ALL) {
                            Logger.logEvent("Connection is closed by DHT.");
                        }
                        break;
                    }
                    validity = Protocol.sizeCheck(messageToKX) > 0;
                    if (!validity || Configuration.LOG_ALL) {
                        Logger.logEvent((validity ? "" : "IN") 
                                + "VALID MESSAGE TO KX:\n" + messageToKX);
                    }
                    publicOut.write(messageFromKX);
                } catch (SocketTimeoutException ex) {// This is the timeout Event. Don't do anything.
                }
            }
            publicIn.close();
            kxOut.close();
            publicSocket.close();
            kxSocket.close();
        } catch (Exception ex) {
            Logger.logEvent("KX Proxy reached failure. error details: " + 
                    ex.getMessage());
        }
    }
}
