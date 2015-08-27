/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sandbox;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import protocol.Configuration;
import protocol.Protocol;
import tools.Logger;
import tools.Server;

/**
 *
 * @author Emertat
 */
public class DummyKX extends Server {

    Configuration conf;

    public DummyKX(String confFile) {
        conf = new Configuration(new File(confFile));
        socketBuffer = new ArrayList<>();
        startServer(conf.getKXPort());
    }

    @Override
    public void run() {
        try {
            Socket s = socketBuffer.remove(0);
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            String message = Protocol.read(in);
            Protocol.MessageType mt = Protocol.getMessageType(message);
            if (mt.equals(Protocol.MessageType.KX_TN_BUILD_IN)) {
                PrintWriter out = new PrintWriter(s.getOutputStream());
                out.print(Protocol.create_TN_READY(
                        Protocol.get_TN_BUILD_PsuedoIdentity(message),
                        Protocol.get_TN_BUILD_IPv4(message),
                        Protocol.get_TN_BUILD_IPv6(message)));
                out.close();
                s.close();
            } else if (mt.equals(Protocol.MessageType.KX_TN_BUILD_OUT)) {
                PrintWriter out = new PrintWriter(s.getOutputStream());
                out.print(Protocol.create_TN_READY(
                        Protocol.get_TN_BUILD_PsuedoIdentity(message),
                        Protocol.get_TN_BUILD_IPv4(message),
                        Protocol.get_TN_BUILD_IPv6(message)));
                out.close();
                s.close();
            } else if (mt.equals(Protocol.MessageType.KX_TN_DESTROY)) {
//                for now, do nothing. this is a primitive dummy Module.
                s.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public String get(String key) {
        try {
            Socket socket = new Socket(conf.getDHTHost(), conf.getDHTPort());
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream());
            out.print(Protocol.create_DHT_GET(key));
            String reply = Protocol.read(in);
            return reply;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public void put(String key, String content) {
        try {
            Socket socket = new Socket(conf.getDHTHost(), conf.getDHTPort());
            OutputStream out = socket.getOutputStream();
            out.write(Protocol.create_DHT_PUT(key, conf.getDHT_TTL(), conf.getDHT_REPLICATION(), content).array());
            out.close();
            socket.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public void trace(String key) {
        try {
            Socket socket = new Socket(conf.getDHTHost(), conf.getDHTPort());
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream());
            out.println(Protocol.create_DHT_TRACE(key));
            String reply = Protocol.read(in);
            if (Protocol.DHT_TRACE_REPLY_isValid(reply)) {
                if(Configuration.LOG_ALL)
                Logger.logEvent(" Successfully received Trace reply message" +
                        reply);
            }
        } catch (Exception ex) {
                Logger.logEvent(" Trace function of dummy KX failed."
                        + " error details: " + ex.getMessage());
        }
    }
}