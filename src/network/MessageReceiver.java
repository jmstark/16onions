package network;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Emertat
 */
public class MessageReceiver implements Runnable {

    private final Socket clientSocket;
    private BufferedReader in = null;
//    private DataOutputStream out = null;
    private Server s;

    public MessageReceiver(Socket clientSocket, Server s) {
        this.clientSocket = clientSocket;
        this.s = s;
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
//            out = new DataOutputStream(clientSocket.getOutputStream());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void run() {
        String message = "";
        try {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                message += inputLine;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // now the Server which initiated the ConnectionListener, will get the message.
        s.handleMessage(message);
    }
}