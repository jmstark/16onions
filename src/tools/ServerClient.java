/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;
import protocol.Message;

/**
 *
 * @author troll
 */
public abstract class ServerClient implements EventHandler {

    protected SocketChannel channel;

    public abstract boolean writeMessage(ByteBuffer msg);

    public void close() throws IOException {
        this.channel.close();
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
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void connectHandler(SelectableChannel channel, SelectorThread selector) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
