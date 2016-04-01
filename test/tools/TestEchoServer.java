/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Assume;

/**
 *
 * @author totakura
 */
public class TestEchoServer extends Server {    
    private int clientsHandled;
    private final Logger logger;

    public TestEchoServer(SocketAddress SockAddr,
            AsynchronousChannelGroup channelGroup) throws IOException {
        super(SockAddr, channelGroup);
        this.logger = Logger.getLogger("tests.tools.TestEchoServer");
    }

    @Override
    protected void handleNewClient(AsynchronousSocketChannel channel) {
        new EchoServerClient(channel, clientsHandled).start();
        logger.log(Level.INFO, "Clients Handled: {0}", ++clientsHandled);
    }

    private class EchoServerClient {

        private final ByteBuffer buffer;
        private final ReadHandler readHandler;
        private final WriteHandler writeHandler;
        private final AsynchronousSocketChannel channel;
        private final Logger logger;

        EchoServerClient(AsynchronousSocketChannel channel, int clientId) {
            this.channel = channel;
            this.buffer = ByteBuffer.allocate(1024);
            this.readHandler = new ReadHandler();
            this.writeHandler = new WriteHandler();
            this.logger = Logger.getLogger(String.format("ServerClient[%d]", clientId));
        }

        private void start() {
            this.channel.read(buffer, this, readHandler);
        }

        private class ReadHandler implements CompletionHandler<Integer, EchoServerClient> {

            @Override
            public void completed(Integer result, EchoServerClient client) {
                if (result <= 0) {
                    client.disconnect();
                    return;
                }
                buffer.flip();
                logger.log(Level.FINER, "Read {0} bytes", buffer.remaining());
                client.channel.write(buffer, client, writeHandler);
            }

            @Override
            public void failed(Throwable ex, EchoServerClient client) {
                client.disconnect();
            }
        }

        private class WriteHandler implements CompletionHandler<Integer, EchoServerClient> {

            @Override
            public void completed(Integer result, EchoServerClient client) {
                if (result <= 0) {
                    client.disconnect();
                    return;
                }
                if (buffer.hasRemaining()) {
                    client.channel.write(buffer, client, writeHandler);
                } else {
                    buffer.clear();
                    client.channel.read(buffer, client, readHandler);
                }
            }

            @Override
            public void failed(Throwable ex, EchoServerClient client) {
                client.disconnect();
                logger.log(Level.WARNING, "server: exception while writing{0}", ex.toString());
            }
        }

        private void disconnect() {
            try {
                logger.fine("Client Disconnected");
                this.channel.close();
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        final int cores = Runtime.getRuntime().availableProcessors();
        final AsynchronousChannelGroup serverChannelGroup;
        int port;
        serverChannelGroup = AsynchronousChannelGroup.
                withFixedThreadPool(cores / 2,
                        Executors.defaultThreadFactory());
        Server _server = null;
        for (port = 6001; (null == _server) && (port < 6999); port++) {
            try {
                _server
                        = new TestEchoServer(
                                new InetSocketAddress(InetAddress.getByName("localhost"),
                                        port), serverChannelGroup);
            } catch (IOException e) {
                continue;
            }
            break;
        }
        final Server server = _server;
        Assume.assumeTrue(null != server);
        server.start();
        System.out.println(String.format("Server running on port %d", port));
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run(){
                try {
                    server.stop();
                } catch (IOException ex) {
                    Logger.getLogger(TestEchoServer.class.getName()).log(Level.SEVERE, null, ex);
                }
                serverChannelGroup.shutdown();
            }
        });
        while(!serverChannelGroup.awaitTermination(5, TimeUnit.MINUTES));
    }
}
