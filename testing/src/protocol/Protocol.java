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
        API_GOSSIP_VALIDATION(503),
        // NSE API message
        API_NSE_QUERY(520),
        API_NSE_ESTIMATE(521),
        // RPS API messages
        API_RPS_QUERY(540),
        API_RPS_PEER(541),
        //ONION API messages
        API_ONION_TUNNEL_BUILD(560),
        API_ONION_TUNNEL_READY(561),
        API_ONION_TUNNEL_INCOMING(562),
        API_ONION_TUNNEL_DESTROY(563),
        API_ONION_TUNNEL_DATA(564),
        API_ONION_ERROR(565),
        API_ONION_COVER(566),
        //ONION AUTH API messages
        API_AUTH_SESSION_START(600),
        API_AUTH_SESSION_HS1(601),
        API_AUTH_SESSION_INCOMING_HS1(602),
        API_AUTH_SESSION_HS2(603),
        API_AUTH_SESSION_INCOMING_HS2(604),
        API_AUTH_LAYER_ENCRYPT(605),
        API_AUTH_LAYER_DECRYPT(606),
        API_AUTH_LAYER_ENCRYPT_RESP(607),
        API_AUTH_LAYER_DECRYPT_RESP(608),
        API_AUTH_SESSION_CLOSE(609),
        API_AUTH_ERROR(610),
        API_AUTH_CIPHER_ENCRYPT(611),
        API_AUTH_CIPHER_ENCRYPT_RESP(612),
        API_AUTH_CIPHER_DECRYPT(613),
        API_AUTH_CIPHER_DECRYPT_RESP(614),
        //Gossip P2P Messages
        GOSSIP_HELLO(9500),
        GOSSIP_NEIGHBORS(9501),
        GOSSIP_DATA(9502),
        //Onion P2P Messages
        ONION_HELLO(9701),
        ONION_DATA(9700);

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
