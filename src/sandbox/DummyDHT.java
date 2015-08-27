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
import java.nio.ByteBuffer;
import java.util.HashMap;
import tools.Server;
import protocol.Configuration;
import protocol.Protocol;
import protocol.Hop;
import tools.MyRandom;

/**
 *
 * @author Emertat
 */
public class DummyDHT extends Server {

//    ConnectionListener r;
//    String lastMessage;
    Configuration conf;

    public DummyDHT(String confFile) {
        this.conf = new Configuration(new File(confFile));;
        fakeStorage = new HashMap<>();
        startServer(conf.getDHTPort());
//        r = new ConnectionListener(this); // Telling the connectionListener to call 'this' after receiving a message.
//        r.startServer(conf.getDHTPort());
//        System.out.println("dummyDHT server started... Listening on port: " + conf.getDHTPort());
    }
    HashMap<String, String> fakeStorage;

    @Override
    public void run() {
        Socket s = socketBuffer.remove(0);
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            String message = Protocol.read(in);
            Protocol.MessageType mt = Protocol.getMessageType(message);
            if (mt.equals(Protocol.MessageType.DHT_PUT)) { // save it in a local hash
                fakeStorage.put(Protocol.get_DHT_MESSAGE_key(message), Protocol.get_DHT_PUT_content(message));
                in.close();
                s.close();
            } else if (mt.equals(Protocol.MessageType.DHT_GET)) { // answer with local fake storage.
                String content = Protocol.get_DHT_MESSAGE_key(message)
                        + fakeStorage.get(Protocol.get_DHT_MESSAGE_key(message));
                ByteBuffer replyMessage = Protocol.addHeader(content, Protocol.MessageType.DHT_GET_REPLY);
                PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                out.print(replyMessage);
                out.flush();
                out.close();
            } else if (mt.equals(Protocol.MessageType.DHT_TRACE)) {// answer with at a random trace.
                MyRandom rand = new MyRandom();
                Hop hops[] = new Hop[5];
                for (int i = 0; i < 5; i++) { // assuming there was 5 hops in the way
                    hops[i] = new Hop();
                    hops[i].setID(rand.randString(Protocol.IDENTITY_LENGTH));
                    hops[i].setIPv4(rand.randString(Protocol.IPV4_LENGTH));
                    hops[i].setIPv6(rand.randString(Protocol.IPV6_LENGTH));
                    hops[i].setKX_port(Protocol.intFormat(rand.randString(Protocol.PORT_LENGTH)));
                }
                ByteBuffer reply = Protocol.create_DHT_TRACE_REPLY(hops, Protocol.get_DHT_MESSAGE_key(message));
                Socket echoSocket = new Socket(Configuration.LOCAL_HOST, conf.getKXPort());
                PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);
                out.println(reply);
                out.flush();
                out.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    /**
     public void handleMessage(String message, int port) {
     if (0 > Protocol.getMessageSize(message)) {
     System.out.println("DHT RECEIVED A MESSAGE WITH WRONG SIZE");
     return;
     }

     switch (Protocol.getMessageType(message)) {
     case DHT_GET:
     // answer with a GET_REPLY MESSAGE.
     String content = Protocol.get_DHT_MESSAGE_key(message)
     + fakeStorage.get(Protocol.get_DHT_MESSAGE_key(message));
     String replyMessage = Protocol.addHeader(content, Protocol.MessageType.DHT_GET_REPLY);
     try {
     Socket echoSocket = new Socket(Configuration.LOCAL_HOST, conf.getKXPort());
     PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);
     out.println(replyMessage);
     out.flush();
     out.close();
     } catch (Exception ex) {
     ex.printStackTrace();
     }
     break;
     case DHT_PUT: // save it in a local hash
     fakeStorage.put(Protocol.get_DHT_MESSAGE_key(message), Protocol.get_DHT_PUT_content(message));
     break;
     case DHT_TRACE: // should answer with at least a random trace.
     //                Protocol.DHT_TRACE_isValid(message);
     MyRandom rand = new MyRandom();
     Hop hops[] = new Hop[5];
     for (int i = 0; i < 5; i++) { // assuming there was 5 hops in the way
     hops[i] = new Hop();
     hops[i].setID(rand.randString(Protocol.PEER_ID_LENGTH));
     hops[i].setIPv4(rand.randString(Protocol.IPV4_LENGTH));
     hops[i].setIPv6(rand.randString(Protocol.IPV6_LENGTH));
     hops[i].setKX_port(Protocol.intFormat(rand.randString(Protocol.KX_PORT_LENGTH)));
     }
     String reply = Protocol.create_DHT_TRACE_REPLY(hops, Protocol.get_DHT_MESSAGE_key(message));
     try {
     Socket echoSocket = new Socket(Configuration.LOCAL_HOST, conf.getKXPort());
     PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);
     out.println(reply);
     out.flush();
     out.close();
     } catch (Exception ex) {
     ex.printStackTrace();
     }
     break;
     default:
     System.out.println("DHT SHOULD NOT RECEIVE THIS MESSAGE TYPE");
     }
     }
     */
}
