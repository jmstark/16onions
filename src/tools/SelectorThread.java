/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import java.io.IOException;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author troll
 */
public class SelectorThread implements Runnable {

    private Selector selector;
    private SelectionKey[] keys;

    public SelectorThread() throws IOException {
        selector = Selector.open();
    }

    public void addChannel(SelectableChannel ch,
            int ops,
            EventProcessor processor) throws ClosedChannelException {
        ch.register(this.selector, ops, processor);
    }

    /**
     * Wakeup this selector thread. If the selector is not blocked, any future
     * select calls will be unblocked immediately.
     */
    public void wakeup() {
        this.selector.wakeup();
    }

    @Override
    public void run() {
        int ready;
        while (true) {
            try {
                ready = this.selector.select();
            } catch (IOException ex) {
                Logger.getLogger(SelectorThread.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
            if (0 == ready) //select() was woken up.
            {
                return;
            }
            this.dispatch();
        }
    }

    private void dispatch() {
        SelectionKey readyKey;
        Set<SelectionKey> readySet = this.selector.selectedKeys();
        Iterator<SelectionKey> iter = readySet.iterator();
        EventProcessor processor;
        int readyOps;
        boolean requeue;
        while (iter.hasNext()) {
            readyKey = iter.next();
            
            readyOps = readyKey.readyOps();
            processor = (EventProcessor) readyKey.attachment();
            requeue = processor.process(readyOps, readyKey.channel(), this);
            if (!requeue)
                readyKey.cancel(); 
            iter.remove();
        }
    }
}
