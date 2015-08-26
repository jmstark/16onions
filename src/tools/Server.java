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
public abstract class Server implements Runnable {

    protected static ArrayList<Socket> socketBuffer;

    private class Listener implements Runnable {

        private Server serverImpl;
        private int port;
        private ExecutorService clientProcessingPool;

        Listener(Server serverImpl, final int port) {
            this.serverImpl = serverImpl;
            this.port = port;
            this.clientProcessingPool = Executors.newFixedThreadPool(10);
        }

        @Override
        public void run() {
            try {
                System.out.println("Going to start a server with port: " + this.port);
                ServerSocket serverSocket = new ServerSocket(this.port);
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    socketBuffer.add(clientSocket);
                    clientProcessingPool.submit(this.serverImpl);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void startServer(final int port) {
        Listener listener = new Listener(this, port);
        Thread listenThread = new Thread(listener);
        listenThread.start();
    }
}
