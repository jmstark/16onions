/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sandbox;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import protocol.Configuration;
import protocol.Protocol;

/**
 *
 * @author Emertat
 */
public class DummyKX implements Runnable {

    Configuration conf;

    public DummyKX(String confFile) {
        conf = new Configuration(new File(confFile));
        //socketBuffer = new ArrayList<>();
        //startServer(conf.getKXPort());
    }

    @Override
    public void run() {
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

    public void put(byte[] key, byte[] content) {

    }

    public void trace(String key) {

    }
}