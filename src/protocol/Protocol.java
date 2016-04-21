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

        DHT_PUT(500),
        DHT_GET(501),
        DHT_TRACE(502),
        DHT_GET_REPLY(503),
        DHT_TRACE_REPLY(504),
        DHT_ERROR(505),
        KX_TN_BUILD_IN(600),
        KX_TN_BUILD_OUT(601),
        KX_TN_READY(602),
        KX_TN_DESTROY(603),
        KX_ERROR(604),
        // Gossip API messages
        GOSSIP(700),
        //Gossip P2P Messages
        GOSSIP_HELLO(9700),
        GOSSIP_NEIGHBORS(9701),
        GOSSIP_DATA(9702);

        private final int numVal;

        MessageType(int numVal) {
            this.numVal = numVal;
        }

        public int getNumVal() {
            return numVal;
        }

        public static MessageType asMessageType(int numVal) {
            for (MessageType mtype : MessageType.values()) {
                if (mtype.getNumVal() == numVal) {
                    return mtype;
                }
            }
            return null;
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
