/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package protocol;

/**
 *
 * @author troll
 * @param <A> closure for the callback
 * @param <B> return value required from the @a handleMessage() function in this
 * abstract class
 */
public abstract class MessageHandler<A, B> {

    private final A a;

    public MessageHandler(A a) {
        this.a = a;
    }

    public MessageHandler() {
        this.a = null;
    }

    protected abstract B handleMessage(Message message, A a);

    public final B handleMessage(Message message) {
        return handleMessage(message, a);
    }
}
