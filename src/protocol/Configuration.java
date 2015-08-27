package protocol;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.ini4j.Ini;
import tools.MyRandom;


/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Emertat
 */
public class Configuration {

    public static final boolean DEV_MODE = true;
    public static String LOCAL_HOST = "127.0.0.1";
    public static String LOG_FILE = "log.txt";
    public static String VOIP_CMD;
    public static String KX_CMD;
    public static String DHT_CMD;
    public static boolean LOG_ALL = true;
    public static int kxHops = 3;
    private String hostkey = "";
    private String dht_host = LOCAL_HOST;
    private String kx_host = LOCAL_HOST;
    private String outreach_hostname = LOCAL_HOST;
    private String tun_ip = LOCAL_HOST + "/24";
    private int dht_port = 3001, kx_port = 3002;
    public int voip_external_port = 3003; //TODO: see what to use instead of this?
    public static String peerIdentity = "to be filled"; //TODO: fill this.
    private int dht_ttl = 10; // if no TTL was defined, TTL default.
    private int dht_replication = 5; // if no Replication was defined, default.
    private int outreach_port = 3003;
    private Ini ini;

    /**
     * This constructor does not receive any configuration files. This is good,
     * for when instead of using a .ini file, we want to use the defaults or use
     * setter functions to set the configurations one by one. This will
     * generally be used, when we want to fool other modules and send our proxy
     * in between. This way we can modify some ports.
     */
    public Configuration() {
        ini = new Ini();
    }

    /**
     * This constructor receives a .ini file, parses the content, if something
     * was not in the configuration file or could not be retrieved for any
     * reason, this function assumes the default value for that parameter.
     *
     * @param configFile
     */
    public Configuration(File configFile) {
        try {
            InputStream bis = new BufferedInputStream(new FileInputStream(configFile));
            int c; // the character that is read.
            String global = "";
            while ((c = bis.read()) != '[') {
                global += (char) c;
            }
            bis.mark(0);
            bis.close();
            bis = new BufferedInputStream(new FileInputStream(configFile));
            bis.skip(global.length());
            Ini ini = new Ini(bis);
            this.ini = ini;
            try {
                if (global.contains("=")) {
                    if (global.trim().split("=")[0].trim().equals("HOSTKEY")) {
                        hostkey = global.trim().split("=")[1].trim();
                    }
                }
            } catch (Exception ex) {
            }
            if (ini.get("DHT", "HOSTNAME") != null
                    && ini.get("DHT", "HOSTNAME").length() > 0) {
                dht_host = ini.get("DHT", "HOSTNAME");
            }
            if (ini.get("KX", "HOSTNAME") != null
                    && ini.get("KX", "HOSTNAME").length() > 0) {
                kx_host = ini.get("KX", "HOSTNAME");
            }
            if (ini.get("KX", "OUTREACH_HOSTNAME") != null
                    && ini.get("KX", "OUTREACH_HOSTNAME").length() > 0) {
                outreach_hostname = ini.get("KX", "OUTREACH_HOSTNAME");
            }
            if (ini.get("KX", "TUN_UP") != null
                    && ini.get("KX", "TUN_UP").length() > 0) {
                tun_ip = ini.get("KX", "TUN_UP");
            }
            bis.close();
            try {
                outreach_port = Integer.parseInt(ini.get("KX", "OUTREACH_PORT"));
            } catch (Exception ex) {
            }
            try {
                dht_port = Integer.parseInt(ini.get("DHT", "PORT"));
            } catch (Exception ex) {
            }
            try {
                kx_port = Integer.parseInt(ini.get("KX", "PORT"));
            } catch (Exception ex) {
            }
            try {
                dht_ttl = Integer.parseInt(ini.get("DHT", "TTL"));
            } catch (Exception ex) {
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public String getTunIp() {
        return tun_ip;
    }

    public void setTunIp(String cidr) {
        ini.put("KX", "TUN_IP", cidr);
        tun_ip = cidr;
    }

    public void setOutreachHostname(String host) {
        ini.put("KX", "OUTREACH_HOSTNAME", host);
        outreach_hostname = host;
    }

    public String getOutreachHostname() {
        return outreach_hostname;
    }

    public int getDHTPort() {
        return dht_port;
    }

    public void setOutreachPort(int port) {
        ini.put("KX", "OUTREACH_PORT", port);
        this.outreach_port = port;
    }

    public int getOutreachPort() {
        return outreach_port;
    }

    public void setDHTPort(int port) {
        ini.put("DHT", "PORT", port);
        dht_port = port;
    }

    public String getDHTHost() {
        return dht_host;
    }

    public void setDHTHost(String host) {
        ini.put("DHT", "HOSTNAME", host);
        dht_host = host;
    }

    public int getKXPort() {
        return kx_port;
    }

    public void setKXPort(int port) {
        ini.put("DHT", "PORT", port);
        kx_port = port;
    }

    public String getKXHost() {
        return kx_host;
    }

    public void setKXHost(String host) {
        ini.put("KX", "HOSTNAME", host);
        kx_host = host;
    }

    public int getDHT_TTL() {
        return dht_ttl;
    }

    public void setDHT_TTL(int ttl) {
        ini.put("DHT", "TTL", ttl);
        this.dht_ttl = ttl;
    }

    public int getDHT_REPLICATION() {
        return dht_replication;
    }

    public void setDHT_REPLICATION(int replication) {
        ini.put("DHT", "REPLICATION", replication);
        dht_replication = replication;
    }

    public void setHostkey(String hostkey) {
        this.hostkey = hostkey;
    }

    public String getHostkey() {
        return hostkey;
    }

    public String store() {
        File f;
        File tempDir = new File(".\\temp\\");
        if (!tempDir.exists()) {
            tempDir.mkdir();
        }
        do {
            f = new File(".\\temp\\" + new MyRandom().randLetter(50) + ".ini");
        } while (f.exists());
        try {
            f.createNewFile();
            OutputStream os = new BufferedOutputStream(new FileOutputStream(f));
            os.write(("HOSTKEY = " + hostkey + "\n").getBytes());
            ini.store(os);
//            ini.store(file);
            return f.getAbsolutePath();
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static void main(String args[]) {
        debug();
    }

    private static void debug() {
//        try {
        Configuration conf = new Configuration(new File("conf.ini"));
        conf.setDHTPort(2000);
        conf.setDHTHost("something.two.thousand");
//            File f = new File(new MyRandom().randLetter(50)+ ".ini");
//            System.out.println(f.getName());
//            System.out.println(f.createNewFile());
        conf.store();
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
    }
}