package network;


import java.io.PrintWriter;
import java.net.Socket;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Emertat
 */
public class Sender {
    public static void main(String [] args){
        String hostName ="127.0.0.1";
        int portNumber = 8000;
        try{
            Socket echoSocket = new Socket(hostName, portNumber);
            PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);

//            DataOutputStream out = new DataOutputStream(echoSocket.getOutputStream());
            int i = 0;
            while(i < 10){
                i++;
                out.println("this is my message\n this is the rest of the message");
                Thread.sleep(1000 *3600);
            }
            echoSocket.close();
        } catch(Exception ex){
            ex.printStackTrace();
        }
    }
}
