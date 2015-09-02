/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package protocol;

/**
 *
 * @author totakura
 */
public interface ServerClientMessageHandler {

    /**
     * Function called when a message is received from a client
     *
     * @param message the message
     * @param client the client sending this message
     * @return true to keep the connection to the client active; false to
     * disconnect the client
     */
    public boolean handleMessage(Message message, ProtocolServerClient client);
}
