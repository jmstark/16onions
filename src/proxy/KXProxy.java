/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package proxy;

import java.io.PrintWriter;
import java.net.Socket;
import tools.ConnectionListener;
import tools.Server;
import protocol.Configuration;
import protocol.Protocol;
import scenario.Validation;
import tools.MyRandom;

/**
 * This class impersonates a KX, to listen to messages that KX is supposed to
 * receive.
 *
 * @author Emertat
 */
public class KXProxy implements Server {

    Configuration conf;
    Validation v;
    ConnectionListener cl;
    MyRandom r;

    public KXProxy(Validation v, Configuration conf) {
        this.conf = conf;
        cl = new ConnectionListener(this);
        cl.startServer(conf.getKXPort()); // impersonating KX.
        this.v = v;
        r = new MyRandom();
    }

    public void sendDHT_TRACE(String key) {
        try {
            Socket echoSocket = new Socket(Configuration.LOCAL_HOST, conf.getDHTPort());
            PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);
            String message = Protocol.create_DHT_TRACE(r.randString(Protocol.KEY_LENGTH));
            out.println(message);
            out.flush();
            echoSocket.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public void sendDHT_PUT(String content, String key) {
        try {
            Socket echoSocket = new Socket(Configuration.LOCAL_HOST, conf.getDHTPort());
            PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);
            String message = Protocol.create_DHT_PUT(key, conf.getDHT_TTL(),
                    conf.getDHT_REPLICATION(), content);
            out.println(message);
            out.flush();
            echoSocket.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void sendDHT_GET(String key) {
        try {
            Socket echoSocket = new Socket(Configuration.LOCAL_HOST, conf.getDHTPort()); //creating a socket and normally sending.
            PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);
            String message = Protocol.addHeader(key, Protocol.MessageType.DHT_GET);
            out.println(message); // sending a get request.
            echoSocket.close(); // closing the connection
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void handleMessage(String message, int port) {
        switch (Protocol.getMessageType(message)) {
            case DHT_GET_REPLY:
                if (!Protocol.DHT_GET_REPLY_isValid(message)) {
                    System.out.println("DHT GET REPLY MESSAGE NOT VALID."); // we have an error.
                    return;
                }
                if (v != null) {
                    v.validate(message);
                }
                break;
            case DHT_TRACE_REPLY:
                if (!Protocol.DHT_TRACE_REPLY_isValid(message)) {
                    System.out.println("DHT TRACE REPLY MESSAGE NOT VALID."); // we have an error.
                    return;
                }
                System.out.println("success!");
                if (v != null) {
                    v.validate(message);
                }
                break;
        }
    }
}
