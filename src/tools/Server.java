/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author Emertat
 */
public class Server implements Runnable {

    protected static ArrayList<Socket> socketBuffer;

    @Override
    public void run() {
    }

    protected void startServer(final int port) {
        final ExecutorService clientProcessingPool = Executors.newFixedThreadPool(10);
        Runnable serverTask = new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket serverSocket = new ServerSocket(port);
                    while (true) {
                        Socket clientSocket = serverSocket.accept();
                        socketBuffer.add(clientSocket);
                        clientProcessingPool.submit(this);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        Thread serverThread = new Thread(serverTask);
        serverThread.start();
    }
}
