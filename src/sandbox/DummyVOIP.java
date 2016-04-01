/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sandbox;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import protocol.Configuration;
import protocol.Protocol;

/**
 *
 * @author Emertat
 */
public class DummyVOIP {

    final Logger logger;
    Configuration conf;

    public DummyVOIP(String confFile) {
        conf = new Configuration(new File(confFile));
        logger = Logger.getLogger(this.getClass().getName());
    }

    /**
     * In this function a request to KX is sent to build an outgoing tunnel. If
     * KX manages to do that, the function returns the IP for the tunnel that
     * the caller of the function can use. If KX fails, this function returns
     * null.
     */
    public String buildTunnel(String psuedoidentity, int hops, int port, String ipv4,
            String ipv6, String identity) {
        try {
            Socket socket = new Socket(conf.getKXHost(), conf.getKXPort());
            socket.setSoTimeout(60000); // 60 seconds.
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream());
            out.print(Protocol.create_TN_BUILD(psuedoidentity, hops, port, ipv4, ipv6, identity));
            String reply = Protocol.read(in);
            out.close();
            in.close();
            socket.close();
            return Protocol.get_TN_READY_IPv4(reply);
        } catch (Exception ex) {
            logger.log(Level.WARNING,
                    "Dummy Voip Failed to send tunnel build request." + 
                            "error details: {0}",
                    ex.getMessage());
            return null;
        }
    }

    public void destroyTunnel(String identity) {
        try {
            Socket socket = new Socket(conf.getKXHost(), conf.getKXPort());
            socket.setSoTimeout(60000); // 60 seconds.
            try (PrintWriter out = new PrintWriter(socket.getOutputStream())) {
                out.print(Protocol.create_TN_DESTROY(identity));
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING,
                    " Dummy Voip Failed to send tunnel destroy request." +
                            "error details: {0}",
                    ex.getMessage());
        }
    }

    public void send(String ip, int port, String content) {
        try {
            Socket socket = new Socket(ip, port);
            try (PrintWriter out = new PrintWriter(socket.getOutputStream())) {
                out.print(content);
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING,
                    "Dummy Voip Failed to send data. error details: {0}",
                    ex.getMessage());
        }
    }
}
