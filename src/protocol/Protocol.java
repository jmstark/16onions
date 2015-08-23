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

    public enum MessageType {

        DHT_PUT,
        DHT_GET,
        DHT_TRACE,
        DHT_GET_REPLY,
        DHT_TRACE_REPLY,
        DHT_ERROR,
        KX_TN_BUILD_IN,
        KX_TN_BUILD_OUT,
        KX_TN_READY,
        KX_TN_DESTROY,
        KX_ERROR
    }

    public static String addHeader(String content, MessageType type) {
        return addSize(addType(content, type));
    }

    /**
     *
     * @param rawMessage The Content of the message
     * @param type The MessageType of the message.
     * @return
     */
    private static String addType(String rawMessage, MessageType type) {
        return twoBytesFormat(type.ordinal()) + rawMessage;
    }

    /**
     *
     * @param typedMessage a message that its type has been prepended.
     * @return a string that the size has been prepended to it.
     */
    private static String addSize(String typedMessage) {
//        TODO: add actual size later. right now its random.
        int size = typedMessage.length() + 2; //two bytes for the size field.
        return twoBytesFormat(size) + typedMessage;
    }

    public static String get_DHT_REPLY_key(String message) {
        return message.substring(SIZE_LENGTH + TYPE_LENGTH,
                SIZE_LENGTH + TYPE_LENGTH + KEY_LENGTH);
    }

    /**
     * usable for every kind of DHT messages except DHT_ERROR.
     *
     * @return the key of the message.
     */
    public static String get_DHT_MESSAGE_key(String message) {
        return message.substring(SIZE_LENGTH + TYPE_LENGTH,
                SIZE_LENGTH + TYPE_LENGTH + KEY_LENGTH);
    }

    public static String get_DHT_PUT_content(String message) {
        return message.substring(SIZE_LENGTH + TYPE_LENGTH + KEY_LENGTH
                + TTL_LENGTH + REPLICATION_LENGTH + DHT_PUT_RESERVED_BYTES);
    }

    public static String get_DHT_REPLY_content(String message) {
        return message.substring(SIZE_LENGTH + TYPE_LENGTH + KEY_LENGTH);
    }

    public static boolean DHT_GET_isValid(String message) {
        return false;
    }

    private static MessageType getType(String message) {
        return MessageType.values()[intFormat(message.substring(SIZE_LENGTH))];
    }

    public static boolean DHT_TRACE_isValid(String message) {
        if (getMessageSize(message) == SIZE_LENGTH + TYPE_LENGTH + KEY_LENGTH
                && getType(message) == MessageType.DHT_TRACE) {
            return true;
        }
        return false;
    }

    public static boolean DHT_TRACE_REPLY_isValid(String message) {
        if(((double) message.length() - SIZE_LENGTH - TYPE_LENGTH - KEY_LENGTH)
                / (PEER_ID_LENGTH + KX_PORT_LENGTH
                + DHT_TRACE_REPLAY_RESERVED_BYTES + 
                IPV4_LENGTH + IPV6_LENGTH) % 1 == 0.0){
            return true;
        }
        return false;
    }

    public static boolean DHT_GET_REPLY_isValid(String message) {
        if (getMessageSize(message) < SIZE_LENGTH + TYPE_LENGTH + KEY_LENGTH) {
            return false;
        }
        if (intFormat(message.substring(SIZE_LENGTH, SIZE_LENGTH + TYPE_LENGTH))
                != MessageType.DHT_GET_REPLY.ordinal()) {
            return false;
        }
        return true;
    }

    /**
     * The message sent to the DHT, typically by the KX layer, can be validated.
     *
     * @param message
     * @return
     */
    public static boolean DHT_PUT_isValid(String message) {
        int size = getMessageSize(message);
        if ((size - SIZE_LENGTH - TYPE_LENGTH)
                <= KEY_LENGTH
                + DHT_PUT_RESERVED_BYTES
                + TTL_LENGTH
                + REPLICATION_LENGTH) // LESS OR EQUAL: there should be one content bit.
        {
            return false;
        }
        return true;
    }

    public static String create_DHT_TRACE_REPLY(Hop[] hops, String key) {
        String res = key;
        for (int i = 0; i < hops.length; i++) {
            res += hops[i].toString();
        }
        return addHeader(res, MessageType.DHT_TRACE_REPLY);
    }

    public static String create_DHT_TRACE(String key) {
        return addHeader(key, MessageType.DHT_TRACE);
    }

    /**
     * This function creates a DHT_PUT style message.
     *
     * @param key expected to be 256 bits.
     * @param TTL expected to fit in 16 bits.
     * @param Replication expected to be less than 256.
     * @param content could be a very large content, but not make the message
     * size more than maximum allowed.
     * @return the built message with its header also completed.
     */
    public static String create_DHT_PUT(String key, int TTL, int Replication, String content) {
        String res = key + twoBytesFormat(TTL) + (char) Replication + "00000" + content;
        return addHeader(res, MessageType.DHT_PUT);
    }

    public static MessageType getMessageType(String message) {
        return MessageType.values()[intFormat(message.substring(2, 4))];
    }

    /**
     * this function receives the message, and translates the first two bytes of
     * it into an int, which should be the message size. it also does a
     * primitive check if the declared Size matches the actual message length.
     *
     * @param message the message as a String.
     * @return -1 if message cannot be valid. size of message if message is
     * valid sizewise.
     */
    public static int getMessageSize(String message) {
        if (message.length() < KEY_LENGTH + TYPE_LENGTH) { // at least header.
            return -1;
        }
        if (message.length() > MAX_MESSAGE_SIZE) { // obligation to max size.
            return -1;
        }
        int size = intFormat(message);
        if (size == message.length()) { // delcaring right size.
            return size;
        }
        return -1;
    }

    public static String twoBytesFormat(int num) {
        int a[] = {1, 2, 3};
        char c[] = {(char) ((int) (num / 256) % 256), (char) (num % 256)};
        return new String(c);
    }

    /**
     * receives a String, turns the first two bytes, MSB,LSB, to an integer.
     *
     * @param s the string whose first two bytes are only important.
     * @return the integer that is the result of the calculation. returns -1 if
     * string does not have two characters.
     */
    public static int intFormat(String s) {
        if (s.length() < 2) {
            return -1;
        }
        return (int) s.charAt(0) * 256 + (int) s.charAt(1);
    }
    // <editor-fold defaultstate="opened" desc="attributes">
    /**
     * Size of the 'size' section of message, in bytes.
     */
    public static int SIZE_LENGTH = 2;
    /**
     * Size of the header section of message, in bytes.
     */
    public static int TYPE_LENGTH = 2;
    /**
     * Size of the key section of messages, in bytes.
     */
    public static int KEY_LENGTH = 32;
    /**
     * this is for the DHT_PUT_MESSAGE
     */
    public static int TTL_LENGTH = 2;
    /**
     * this is for the DHT_PUT_MESSAGE
     */
    public static int REPLICATION_LENGTH = 1;
    public static int DHT_PUT_RESERVED_BYTES = 5;
    public static int PEER_ID_LENGTH = 32;
    public static int KX_PORT_LENGTH = 2;
    public static int DHT_TRACE_REPLAY_RESERVED_BYTES = 2;
    public static int DHT_TRACE_REPLY_RESERVED_BYTES = 2;
    public static int IPV4_LENGTH = 4;
    public static int IPV6_LENGTH = 16;
    public static int MAX_MESSAGE_SIZE = 64000; // IS THIS CORRECT?
    public static int MAX_VALID_CONTENT = 62000; // TODO: calculate correctly
    // </editor-fold>
}
