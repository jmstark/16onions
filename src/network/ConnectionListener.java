package network;


import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Emertat
 */
public class ConnectionListener {
    public static void main(String[] args) {
        new ConnectionListener(new Server() {

            @Override
            public void handleMessage(String message) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        }).startServer(8000);
    }
    Server server;

    public ConnectionListener(Server s) {
        this.server = s;
    }
    public void startServer(final int port) {
        final ExecutorService clientProcessingPool = Executors.newFixedThreadPool(10);
        Runnable serverTask = new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket serverSocket = new ServerSocket(port);
//                    System.out.println("Waiting for clients to connect...");
                    while (true) {
                        Socket clientSocket = serverSocket.accept();
                        clientProcessingPool.submit(new MessageReceiver(clientSocket, server));
                    }
                } catch (Exception e) {
                    System.err.println("Unable to process client request");
                    e.printStackTrace();
                }
            }
        };
        Thread serverThread = new Thread(serverTask);
        serverThread.start();
    }
}
