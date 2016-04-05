
import java.io.File;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import protocol.Configuration;
import protocol.Protocol;
import proxy.DHTProxy;
import proxy.KXProxy;
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

    public static ArrayList<Integer> freePorts;

    /**
     * each run of the program starts here. each time, only one scenario is
     * executed when complete, the program will receive a .config file address,
     * a dht, a kx, and a voip file address. and a log file address for the test
     * results. Run the scenario according to the inputs, and write the result
     * in the log file.
     *
     * @param args
     */
    public static void main(String args[]) {
        Logger logger = Logger.getLogger(Main.class.getName());
        logger.setLevel(Level.ALL);
        logger.info("Test message");
    }
}
