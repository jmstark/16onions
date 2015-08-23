
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
import scenario.*;

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
        args = new String[6]; // this is temp.
        args[0] = "-scenario";
        args[1] = "1";
        args[2] = "-conf";
        args[3] = "conf.ini";
        args[4] = "-free";
        args[5] = "100..200";
        try {
            //        new Scenario2();
            CommandLineParser clp = new DefaultParser();
            Options options = new Options();
            options.addOption("kx", true, "kx module");
            options.addOption("dht", true, "dht module");
            options.addOption("voip", true, "voip module");
            options.addOption("conf", true, "Configuration file address");
            options.addOption("scenario", true, "Scenario number.");
            options.addOption("free", true, "inclusive range of free ports. for example: one thousand to two thousand: 1000..2000."
                    + " Default range for test module is 9000 to 9100.");
            CommandLine cl = clp.parse(options, args);
//            Configuration conf = new Configuration(new File());
            System.out.println(cl.getOptionValue("conf"));
            String portRange = cl.getOptionValue("free");
            int start;
            int end;
            try {
                start = Integer.parseInt(portRange.split("..")[0]);
                end = Integer.parseInt(portRange.split("..")[1]);
            } catch (Exception ex) { // No or bad range provided.
                start = 9000;
                end = 9100;
            }
            freePorts = new ArrayList<>();
            for(int i = start; i <= end; i++){
                freePorts.add(i);
            }
            new Scenario4(cl.getOptionValue("conf"), freePorts);
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
    }
}
