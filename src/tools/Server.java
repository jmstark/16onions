/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Emertat
 */
public class Server implements EventHandler {

    private final ServerSocketChannel serverChannel;
    private SelectorThread selector;
    private final ServerHandler handler;
    private Thread serverThread;
    private final HashMap<SocketChannel, ServerClient> clientMap;
    private boolean hasOwnSelector;

    public Server(ServerHandler handler, SocketAddress SockAddr) throws IOException {
        serverChannel = ServerSocketChannel.open();
        serverChannel.bind(SockAddr);
        serverChannel.configureBlocking(false);
        this.handler = handler;
        this.clientMap = new HashMap(50);
    }

    public void start(SelectorThread selector) {
        this.selector = selector;
        try {
            selector.addChannel(serverChannel, SelectionKey.OP_ACCEPT, this);
            serverThread = new Thread(selector);
            serverThread.start();
        } catch (ClosedChannelException | ChannelAlreadyRegisteredException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void start() throws IOException {
        SelectorThread selector = new SelectorThread();
        this.hasOwnSelector = true;
        this.start(selector);
    }

    public void stop() throws IOException {
        if (this.hasOwnSelector) {
            this.selector.wakeup();
        }
        Iterator<ServerClient> iter = this.clientMap.values().iterator();
        ServerClient client;
        while (iter.hasNext()) {
            client = iter.next();
            client.close();
        }
        this.serverChannel.close();
        while (true) {
            try {
                this.serverThread.join();
                break;
            } catch (InterruptedException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void readHandler(SelectableChannel channel, SelectorThread selector) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeHandler(SelectableChannel channel, SelectorThread selector) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void acceptHandler(SelectableChannel channel, SelectorThread selector) {
        try {
            ServerSocketChannel serverSocket = (ServerSocketChannel) channel;
            SocketChannel clientSocket;
            ServerClient client;

            clientSocket = serverSocket.accept();
            client = new ServerClientImpl(clientSocket, this);
            this.clientMap.put(clientSocket, client);
            if (!this.handler.newConnectionHandler(client)) {
                cleanupServerClient(clientSocket);
                return;
            }
            clientSocket.configureBlocking(false);
            this.selector.addChannel(clientSocket, SelectionKey.OP_READ, client);
        } catch (IOException | ChannelAlreadyRegisteredException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void cleanupServerClient(SocketChannel socket) {
        this.clientMap.remove(socket);
        try {
            socket.close();
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void disconnectClientSocket(SocketChannel socket) {
        ServerClient client = this.clientMap.get(socket);
        this.handler.disconnectHandler(client);
        cleanupServerClient(socket);
    }

    @Override
    public void connectHandler(SelectableChannel channel, SelectorThread selector) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private class ServerClientImpl extends ServerClient implements EventHandler {
        
        private final Server server;
        private boolean writePending;
        private final LinkedList<ByteBuffer> messages;
        private boolean readPending;

        ServerClientImpl(SocketChannel channel, Server server) {
            this.channel = channel;
            this.server = server;
            this.messages = new LinkedList();
            this.readPending = true;
        }

        private void refreshInterestOps() throws ChannelNotRegisteredException {
            int ops = 0;
            if (this.readPending) {
                ops |= SelectionKey.OP_READ;
            }
            if (this.writePending) {
                ops |= SelectionKey.OP_WRITE;
            }
            if (0 == ops) {
                this.disconnect();
                return;
            }
            this.server.selector.modifyChannelInterestOps(this.channel, ops);
        }

        @Override
        public boolean writeMessage(ByteBuffer msg) {
            this.writePending = true;
            try {
                this.refreshInterestOps();
            } catch (ChannelNotRegisteredException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
            this.messages.add(msg);
            return true;
        }

        @Override
        public void readHandler(SelectableChannel channel, SelectorThread selector) {
            SocketChannel socket;
            ByteBuffer buffer = ByteBuffer.allocate(5);
            int nread = 0;
            int ops = 0;

            socket = (SocketChannel) channel;
            try {                
                nread = socket.read(buffer);
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                this.disconnect();
            }
            if (nread == -1) {
                this.disconnect();
                this.readPending = false;
                this.writePending = false;
                return;
            }
            buffer.flip();
            this.server.handler.messageHandler(this, buffer);
        }

        @Override
        public void writeHandler(SelectableChannel channel, SelectorThread selector) {
            SocketChannel socket = (SocketChannel) channel;
            ByteBuffer msg;            

            msg = this.messages.peek();
            if (null != msg) {
                try {
                    socket.write(msg);
                    if (!msg.hasRemaining()) {
                        this.messages.remove();
                    }
                } catch (IOException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                    this.disconnect();
                    return;
                }
            }
            if (this.messages.isEmpty()) {
                this.writePending = false;
            }
            try {
                this.refreshInterestOps();
            } catch (ChannelNotRegisteredException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                this.disconnect();
            }
        }

        private void disconnect() {
            this.server.disconnectClientSocket(channel);
        }
    }
}
