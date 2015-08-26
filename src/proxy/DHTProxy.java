/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package proxy;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import tools.Server;
import protocol.Configuration;
import protocol.Protocol;
import protocol.ProtocolException;
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
//            publicSocket.setSoTimeout(2000);
//            dhtSocket.setSoTimeout(2000);
            DataInputStream publicIn = new DataInputStream(publicSocket.getInputStream());
            BufferedReader dhtIn = new BufferedReader(new InputStreamReader(dhtSocket.getInputStream()));
            PrintWriter publicOut = new PrintWriter(publicSocket.getOutputStream());
            OutputStream dhtOut = dhtSocket.getOutputStream();
            if (Configuration.LOG_ALL) {
                Logger.logEvent("A connection to DHT is established from port "
                        + publicSocket.getPort() + " On "
                        + publicSocket.getInetAddress().getHostName() + "("
                        + publicSocket.getInetAddress().getHostAddress() + ")");
            }
            while (true) {
                ByteBuffer messageToDHT;
                try {
                    messageToDHT = Protocol.readMsg(publicIn);
                    dhtOut.write(messageToDHT.array());
                } catch (ProtocolException proto_exp) {
                    proto_exp.printStackTrace();
                    Logger.logEvent("Protocol exception while reading DHT message");
                }                                
                String messageFromDHT = Protocol.read(dhtIn);
                if (messageFromDHT.length() == 0) { // DHT has closd socket.
                    if (Configuration.LOG_ALL) {
                        Logger.logEvent("Connection is closed by DHT.");
                    }
                    break;
                }             
                publicOut.write(messageFromDHT);
            }
        } catch (Exception ex) {
            Logger.logEvent("DHT Proxy reached failure. error details: "
                    + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
