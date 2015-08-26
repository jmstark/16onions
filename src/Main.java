
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
        if (Configuration.DEV_MODE) {
            args = new String[9];
            args[0] = "-scenario";
            args[1] = "1";
            args[2] = "-conf";
            args[3] = "conf.ini";
            args[4] = "-free";
            args[5] = "8000..8100";
            args[6] = "-log";
            args[7] = "log.txt";
            args[8] = "-logall";
        }
        try {
            //        new Scenario2();
            CommandLineParser clp = new DefaultParser();
            Options options = new Options();
            options.addOption("kx", true, "string needed to call KX module. prepare the string to add a configuration file in the end.");
            options.addOption("dht", true, "string needed to call DHT module. prepare the string to add a configuration file in the end.");
            options.addOption("voip", true, "string needed to call VOIP module. prepare the string to add a configuration file in the end.");
            options.addOption("conf", true, "Configuration file address");
            options.addOption("scenario", true, "Scenario number.");
            options.addOption("free", true, "inclusive range of free ports. for example: one thousand to two thousand: 1000..2000."
                    + " Default range for test module is 9000 to 9100.");
            options.addOption("log", true, "address of the log file.");
            options.addOption("logall", false, "Enabling the logging of every "
                    + "Single Transaction.");
            CommandLine cl = clp.parse(options, args);

            if (cl.getOptionValue("log") != null) {
                Configuration.LOG_FILE = cl.getOptionValue("log");
            }
            Configuration.DHT_CMD = cl.getOptionValue("dht");
            Configuration.KX_CMD = cl.getOptionValue("kx");
            Configuration.VOIP_CMD = cl.getOptionValue("voip");
            int start = 9000;
            int end = 9100;
            try {
                String portRange = cl.getOptionValue("free");
                start = Integer.parseInt(portRange.split("..")[0]);
                end = Integer.parseInt(portRange.split("..")[1]);
            } catch (Exception ex) { // No or bad range provided.
            }
            freePorts = new ArrayList<>();
            for (int i = start; i <= end; i++) {
                freePorts.add(i);
            }
            switch (cl.getOptionValue("scenario")) {
                case "1":
                    new Scenario1(cl.getOptionValue("conf"), freePorts);
                    break;
                case "2":
                    new Scenario2(cl.getOptionValue("conf"), freePorts);
                    break;
                case "3":
                    new Scenario3(cl.getOptionValue("conf"), freePorts);
                    break;
                case "4":
                    new Scenario4(cl.getOptionValue("conf"), freePorts);
                    break;
                case "5":
                    new Scenario5(cl.getOptionValue("conf"), freePorts);
                    break;
                case "6":
                    new Scenario6(cl.getOptionValue("conf"), freePorts);
                    break;
            }
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
    }
}
