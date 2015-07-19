/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package protocol;

/**
 *
 * @author Emertat
 */
public class Protocol {
    public static String addHeader(String content, Configuration.MessageType type){
        String res = "";
        switch (type){
            case MSG_DHT_GET:
                res += addSize(addType(content, type));
//                TODO: include and address all message types.
        }
        return res;
    }
    private static String addType(String rawMessage, Configuration.MessageType type){
//        TODO: assign numbers to types and then complete here.
        return "01"+ rawMessage;
    }
    /**
     * 
     * @param typedMessage a message that its type has been prepended.
     * @return a string that the size has been prepended to it.
     */
    private static String addSize(String typedMessage){
//        TODO: add actual size later. right now its random.
        return "12" + typedMessage;
    }
    public static boolean DHT_GET_isValid(String message){
        return false;
    }
    public static boolean DHT_TRACE_isValid(String message){
        return false;
    }
    public static boolean DHT_GET_REPLY_isValid(String message){
        return false;
    }
    public static boolean DHT_PUT_isValid(String message) {
        if (message.length() < (Protocol.KEY_LENGTH)
                + Protocol.SIZE_LENGTH
                + Protocol.TYPE_LENGTH) {
            return false;
        }
        if(message.length() != getMessageSize(message)){
            return false;
        }
        return true;
    }
/**
 * this function receives the message, and translates the first two bytes of it
 * into an int. TODO: this has to depend on Size filed configuration. so if SIZE field changed
 * from two bytes to for example four bytes, this function should change behavior.
 * do that later.
 * @param message the message as a String.
 * @return the the length of the 
 */
    private static int getMessageSize(String message) {
        return message.getBytes()[0] << 8 | message.getBytes()[1];
    }
    /**
     * Size of the 'size' section of message, in bytes.
     */
    public static int SIZE_LENGTH = 2;
    /**
     * Size of the header section of message, in bytes.
     */
    public static int TYPE_LENGTH = 2;
    /**
     * Size of the key section of message, in bytes.
     */
    public static int KEY_LENGTH = 32;
}
