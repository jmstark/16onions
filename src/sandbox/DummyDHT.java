/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sandbox;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import protocol.Configuration;
import protocol.Connection;
import protocol.Message;
import protocol.ProtocolServer;
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
    private HashMap<DHTKey, DHTContent> storage;

    public DummyDHT(SocketAddress socketAddress,
            AsynchronousChannelGroup channelGroup) throws IOException {
        super(socketAddress, channelGroup);
    }

    @Override
    protected boolean handleMessage(Message message, Connection client) {
        DhtMessage dhtMsg = (DhtMessage) message;
        DHTKey key = dhtMsg.getKey();
        switch (message.getType()) {
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
                if (null != content) {
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
                logger.log(Level.WARNING, "Unexpected DHT message received");
                break;
            default:
        }
        return false;
    }

    public static DummyDHT instantiate(Configuration conf) throws IOException {
        InetSocketAddress socketAddress;
        socketAddress = new InetSocketAddress(conf.getDHTHost(), conf.getDHTPort());
        AsynchronousChannelGroup channelGroup;
        channelGroup = AsynchronousChannelGroup.withThreadPool(Executors.newSingleThreadExecutor());
        return new DummyDHT(socketAddress, channelGroup);
    }
}
