/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import java.nio.channels.SelectableChannel;

/**
 *
 * @author troll
 */
public interface EventHandler {

    public void readHandler(SelectableChannel channel, SelectorThread selector);

    public void writeHandler(SelectableChannel channel, SelectorThread selector);

    public void acceptHandler(SelectableChannel channel, SelectorThread selector);

    public void connectHandler(SelectableChannel channel, SelectorThread selector);
}
