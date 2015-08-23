/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sandbox;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import tools.ConnectionListener;
import tools.Server;
import protocol.Configuration;
import protocol.Protocol;
import protocol.Hop;
import tools.MyRandom;

/**
 *
 * @author Emertat
 */
public class DummyDHT implements Server {

    ConnectionListener r;
    String lastMessage;
    Configuration conf;
    public DummyDHT(Configuration conf) {
        this.conf = conf;
        r = new ConnectionListener(this); // Telling the connectionListener to call 'this' after receiving a message.
        r.startServer(conf.getDHTPort());
        System.out.println("dummyDHT server started... Listening on port: " + conf.getDHTPort());
    }
    HashMap<String, String> data = new HashMap<>();

    @Override
    public void handleMessage(String message, int port) {
        if (0 > Protocol.getMessageSize(message)) {
            System.out.println("DHT RECEIVED A MESSAGE WITH WRONG SIZE");
            return;
        }

        switch (Protocol.getMessageType(message)) {
            case DHT_GET:
                // answer with a GET REPLY MESSAGE.
                String content = Protocol.get_DHT_MESSAGE_key(message)
                        + data.get(Protocol.get_DHT_MESSAGE_key(message));
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
                data.put(Protocol.get_DHT_MESSAGE_key(message), Protocol.get_DHT_PUT_content(message));
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
}
