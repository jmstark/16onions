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
public interface EventProcessor {
    public boolean process (int readyOps, SelectableChannel channel, SelectorThread selector);
}
