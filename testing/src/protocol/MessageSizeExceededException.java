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
public class MessageSizeExceededException extends Exception {

    public MessageSizeExceededException(
            String msg) {
        super(msg);
    }

    public MessageSizeExceededException() {
        super();
    }

}
