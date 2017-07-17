/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
package protocol;

/**
 * Class to handle connection breaks. A connection could be broken due to
 * underlying network problems, communication end point going down, or due to
 * protocol exceptions raising from sending invalid protocol messages.
 *
 * @param<A> Type for the closure. The closure will be passed as an argument to
 * the callback
 */
public abstract class DisconnectHandler<A> {

    private final A closure;

    /**
     * Create a disconnect handler with a closure. The callback @a
     * handleDisconnect() will be called with the given closure.
     *
     * @param closure
     */
    public DisconnectHandler(A closure) {
        this.closure = closure;
    }

    /**
     * Callback to be called when a connection is about to be disconnected. The
     * connection will be disconnected after this callback returns.
     *
     * @param connection
     * @param closure
     */
    protected abstract void handleDisconnect(A closure);

    /**
     * Call the main handler function with the object's closure. This function
     * is called by the Connection class.
     */
    final void handleDisconnect() {
        handleDisconnect(this.closure);
    }
}
