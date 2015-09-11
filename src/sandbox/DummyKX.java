/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sandbox;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import protocol.Configuration;
import protocol.Connection;
import protocol.Message;
import protocol.ProtocolServer;
import protocol.kx.*;

/**
 *
 * @author Emertat
 */
public class DummyKX extends ProtocolServer {

    private static final Logger logger = Logger.getLogger(DummyKX.class.getName());
    Configuration conf;
    byte[] ipv4_address;
    byte[] ipv6_address;

    public DummyKX(SocketAddress socketAddress,
            AsynchronousChannelGroup channelGroup) throws IOException {
        super(socketAddress, channelGroup);
        NetworkInterface netdev;
        netdev = NetworkInterface.getByIndex(0);
        if (null == netdev)
        {
            logger.log(Level.WARNING, "no network interfaces found");
            return;
        }
        Enumeration<InetAddress> addresses = netdev.getInetAddresses();
        while(addresses.hasMoreElements()){
            InetAddress address = addresses.nextElement();
            if (null != ipv6_address && address instanceof Inet6Address) {
                ipv6_address = address.getAddress();
                continue;
            }
            if (null != ipv4_address && address instanceof Inet4Address) {
                ipv4_address = address.getAddress();
            }
            if (null != ipv4_address && null != ipv6_address)
                break;
        }
    }

    @Override
    protected boolean handleMessage(Message message, Connection connection) {
        KxTunnelReadyMessage tunReadyMsg;
        KxTunnelBuildMessage buildMsg;
        switch (message.getType()) {
            case KX_TN_BUILD_IN: {
                buildMsg = (KxTunnelBuildMessage) message;
                // for incoming tunnels we respond with NULL ipv4 and ipv6
                tunReadyMsg = new KxTunnelReadyMessage(buildMsg.getPseudoID(),
                        new byte[4], new byte[16]);
                connection.sendMsg(tunReadyMsg);
            }
            case KX_TN_BUILD_OUT: {
                buildMsg = (KxTunnelBuildMessage) message;
                tunReadyMsg = new KxTunnelReadyMessage(buildMsg.getPseudoID(),
                    ipv4_address, ipv6_address);
            }
            case KX_TN_DESTROY:
                logger.info("Received TUN_DESTROY.");
                return true;
            default:
                logger.log(Level.WARNING, "Received wrong message: {0}", message.getType().name());
        }
        return false;
    }
}
