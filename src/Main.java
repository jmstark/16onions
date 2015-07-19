
import java.io.PrintWriter;
import java.net.Socket;
import protocol.Configuration;
import protocol.Protocol;
import sandbox.DummyDHT;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Emertat
 */
public class Main {
/**
 * each run of the program starts here. each time, only one scenario is executed
 * when complete, the program will receive a config file address, a dht, a kx, 
 * and a voip file address. and a log file address for the test results. Run the
 * scenario according to the inputs, and write the result in the log file.
 * @param args 
 */
    public static void main(String args[]) {
        scenario1("no conf file right now.conf");
    }

    /**
     * This function initiates
     *
     * @param args
     */
    public static void startPolicy(String args[]) {
        // TODO: later call this function in Main.
        // TODO: we have to properly parse the args and get the address of KX,VOUP,DHT modules.

//        String confFile = args[0];        
//        String scenario = args[1];
//        TODO: switch case on scenario. to run different scenarios.
    }
/**
 * scenario1 tests if a DHT Module can listen to a message, properly.
 * TODO: needs completion.
 * @param confFile 
 */
    private static void scenario1(String confFile) {
//        Configuration cr = new Configuration(new File(confFile));
        // creating dummyDHT, and make it listen for connections in a new thread.
        int portNumber = 8000;
        DummyDHT dmh = new DummyDHT(portNumber);
        // send messagess to it.
        try {
            Socket echoSocket = new Socket(Configuration.LOCAL_HOST, portNumber);
            PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);
            out.println(Protocol.addHeader("this is a random and probably invalid message.", Configuration.MessageType.MSG_DHT_PUT));
            echoSocket.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    /**
     * In this function we test the DHT's ability to reply to a GET message. we wait for its GET_REPLY message.
     * TODO: needs completion.
     * @param confFile 
     */
    private static void scenario2(String confFile){
        int portNumber = 8000; // This is temp
        try {
            //TODO: create a Listener: A listener, impersonating a KX module.
            Socket echoSocket = new Socket(Configuration.LOCAL_HOST, portNumber); //creating a socket and normally sending.
            PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);
            out.println(Protocol.addHeader("this is the key",
                    Configuration.MessageType.MSG_DHT_GET));// sending a get request.
            echoSocket.close(); // closing the connection
            // this thread normally finishes here, and there is a busy wait in our KX Listener, we evaluate the result there.
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
