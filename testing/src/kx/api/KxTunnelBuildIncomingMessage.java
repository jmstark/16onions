/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kx.api;

import protocol.Hop;
import protocol.Protocol;

/**
 *
 * @author troll
 */
public class KxTunnelBuildIncomingMessage extends KxTunnelBuildMessage {

    public KxTunnelBuildIncomingMessage(byte nhops, byte[] pseudoID, Hop exchangePoint) {
        super(nhops, pseudoID, exchangePoint);
        this.addHeader(Protocol.MessageType.KX_TN_BUILD_IN);
    }

}
