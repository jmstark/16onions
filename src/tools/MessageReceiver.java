package tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import protocol.Protocol;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Emertat
 */
public class MessageReceiver implements Runnable {

    private BufferedReader in = null;
    private Server s;
    private int port;

    public MessageReceiver(Socket clientSocket, Server s, int port) {
        this.s = s;
        this.port = port;
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void run() {
        String message = "";
        char[] buffer = new char[Protocol.MAX_MESSAGE_SIZE];
        int valid = 0;
        try {
            while (valid >= 0) {
                valid = in.read(buffer, 0, Protocol.MAX_MESSAGE_SIZE);
                message += new String(buffer, 0, Math.max(valid, 0));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // now the Server which initiated the ConnectionListener, will get the message.
        // note: two last bytes of the message are just new line characted added by 
        // in.read function to show the end of the message. hence, we are excluding them.
        s.handleMessage(message.substring(0, message.length() - 2), port);
    }
}