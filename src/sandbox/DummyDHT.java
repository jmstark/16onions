/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sandbox;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import protocol.Configuration;
import protocol.Message;
import protocol.ProtocolServer;
import protocol.ProtocolServerClient;
import protocol.dht.DHTContent;
import protocol.dht.DHTKey;
import protocol.dht.DhtGetMessage;
import protocol.dht.DhtGetReplyMessage;
import protocol.dht.DhtMessage;
import protocol.dht.DhtPutMessage;
import protocol.dht.DhtTraceReplyMessage;

/**
 *
 * @author Emertat
 * @author totakura
 */
public class DummyDHT extends ProtocolServer {
    private static final Logger logger = Logger.getLogger(ProtocolServer.class.getName());

    public DummyDHT (SocketAddress socketAddress,
            AsynchronousChannelGroup channelGroup,
            Configuration config) throws IOException {
        super(socketAddress, channelGroup);
    }

    private HashMap<DHTKey, DHTContent> storage;

    @Override
    public boolean handleMessage(Message message, ProtocolServerClient client) {
        DhtMessage dhtMsg = (DhtMessage) message;
        DHTKey key = dhtMsg.getKey();
        switch(message.getType()) {
            case DHT_PUT: {
                DhtPutMessage putMsg = (DhtPutMessage) message;
                DHTContent content = putMsg.getContent();
                //ignore replication and TTL
                storage.put(key, content);
                return true;
            }
            case DHT_GET: {
                DhtGetMessage getMsg = (DhtGetMessage) message;
                DHTContent content = storage.get(key);
                if (null != content)
                {
                    DhtGetReplyMessage getReplyMsg;
                    getReplyMsg = new DhtGetReplyMessage(key, content);
                    client.sendMsg(getReplyMsg);
                }
                return true;
            }
            case DHT_TRACE: {
                DhtTraceReplyMessage traceReplyMsg;
                traceReplyMsg = new DhtTraceReplyMessage(key);
                client.sendMsg(traceReplyMsg);
                return true;
            }
            case DHT_GET_REPLY:
            case DHT_TRACE_REPLY:
                logger.log (Level.WARNING, "Unexpected DHT message received");
                break;
            default:
        }
        return false;
    }
}
