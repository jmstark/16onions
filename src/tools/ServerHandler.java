/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import java.nio.ByteBuffer;
import protocol.Message;

/**
 *
 * @author troll
 */
public interface ServerHandler {
    public boolean newConnectionHandler(ServerClient client);
    public boolean messageHandler(ServerClient client, final ByteBuffer msg);
    public void disconnectHandler(ServerClient client);
}
