/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import java.io.IOException;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author totakura
 */
public final class SelectorThread implements Runnable {

    private final Selector selector;

    public SelectorThread() throws IOException {
        selector = Selector.open();
    }

    public void addChannel(SelectableChannel ch,
            int ops,
            EventHandler processor) throws ClosedChannelException, ChannelAlreadyRegisteredException {
        if (null != ch.keyFor(this.selector)) {
            throw new ChannelAlreadyRegisteredException();
        }
        ch.register(this.selector, ops, processor);
    }

    public void modifyChannelInterestOps(SelectableChannel channel, int ops)
            throws ChannelNotRegisteredException {
        SelectionKey key = channel.keyFor(this.selector);
        if (null == key) {
            throw new ChannelNotRegisteredException();
        }
        key.interestOps(ops);
    }

    public void removeChannel(SelectableChannel ch) throws ChannelNotRegisteredException {
        SelectionKey key = ch.keyFor(this.selector);
        if (null == key) {
            throw new ChannelNotRegisteredException();
        }
        key.cancel();
    }

    /**
     * Wakeup this selector thread. If the selector is not blocked, any future
     * select calls will be unblocked immediately.
     *
     * @throws java.io.IOException
     */
    public void wakeup() throws IOException {
        this.selector.wakeup(); //wakeup();
    }

    @Override
    public void run() {
        int ready;
        while (true) {
            try {
                ready = this.selector.selectNow();
                if (0 == ready) {
                    // if no keys to select, exit
                    if (this.selector.keys().isEmpty()) {
                        return;
                    }
                    ready = this.selector.select();
                }
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
        EventHandler handler;
        SelectableChannel channel;
        while (iter.hasNext()) {
            readyKey = iter.next();
            handler = (EventHandler) readyKey.attachment();
            channel = readyKey.channel();
            iter.remove();
            if (!readyKey.isValid()) {
                continue;
            }
            if (readyKey.isReadable()) {
                handler.readHandler(channel, this);
            }
            if (readyKey.isValid() && readyKey.isWritable()) {
                handler.writeHandler(channel, this);
            }
            if (readyKey.isValid() && readyKey.isAcceptable()) {
                handler.acceptHandler(channel, this);
            }
            if (readyKey.isValid() && readyKey.isConnectable()) {
                handler.connectHandler(channel, this);
            }
        }
    }
}
