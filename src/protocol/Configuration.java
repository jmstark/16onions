package protocol;


import java.io.File;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Emertat
 */
public class Configuration {
    public enum MessageType{
        MSG_DHT_PUT,
        MSG_DHT_GET,
//        TODO: include all message types.
    }
    public static String LOCAL_HOST = "127.0.0.1";
    File conf;
    public Configuration(File configFile) {
        conf = configFile;
    }
    public String getDHTPort(){
        return null;
    }
    public String getDHTHost(){
        return null;        
    }
    public String getVoipPort(){
        return null;
    }
    public String getVoipHost(){
        return null;        
    }
    public String getKXPort(){
        return null;
    }
}