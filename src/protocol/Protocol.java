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

        DHT_PUT(100),
        DHT_GET(101),
        DHT_TRACE(102),
        DHT_GET_REPLY(103),
        DHT_TRACE_REPLY(104),
        DHT_ERROR(105),
        KX_TN_BUILD_IN(200),
        KX_TN_BUILD_OUT(201),
        KX_TN_READY(202),
        KX_TN_DESTROY(203),
        KX_ERROR(204),
        // Gossip API messages
        API_GOSSIP_ANNOUNCE(500),
        API_GOSSIP_NOTIFY(501),
        API_GOSSIP_NOTIFICATION(502),
        //Gossip P2P Messages
        GOSSIP_HELLO(9500),
        GOSSIP_NEIGHBORS(9501),
        GOSSIP_DATA(9502);

        private final int numVal;

        MessageType(int numVal) {
            this.numVal = numVal;
        }

        public int getNumVal() {
            return numVal;
        }

        public static MessageType asMessageType(int numVal) throws
                UnknownMessageTypeException {
            for (MessageType mtype : MessageType.values()) {
                if (mtype.getNumVal() == numVal) {
                    return mtype;
                }
            }
            throw new UnknownMessageTypeException(numVal);
        }
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
     * Size of the header
     */
    public static int HEADER_LENGTH = SIZE_LENGTH + TYPE_LENGTH;

    /**
     * Size of the peer identity
     */
    public static int IDENTITY_LENGTH = 32;

    /**
     * Maximum message size is limited to 64KB
     */
    public static int MAX_MESSAGE_SIZE = 64000;
}
