/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Level;
import protocol.Configuration;

/**
 *
 * @author Emertat
 */
public class Logger {

    private static String specialString = "<event>";
    private static String escapeString = "<escape>";

    /**
     * stuff each message before logging to make sure no accidental special
     * terms are stored and misinterpreted later.
     *
     * @param txt normal text. stuffed.
     * @return stuffed text.
     */
    private static String stuff(String txt) {

        txt = txt.replaceAll(escapeString, escapeString + escapeString);
        txt = txt.replaceAll(specialString, escapeString + specialString);
        return txt;
    }

    /**
     * destuff the text that is read from the log file.
     *
     * @param txt stuffed text.
     * @return destuffed text.
     */
    private static String destuff(String txt) {
        txt = txt.replaceAll(escapeString + specialString, specialString);
        txt = txt.replaceAll(escapeString + escapeString, escapeString);
        return txt;
    }

    public static String[] readEvents(String fileName) {
        String res[] = null;
        try {
            File f = new File(fileName);
            FileInputStream fis = new FileInputStream(f);
            byte b[] = new byte[fis.available()];
            fis.read(b);
            res = new String(b).split("\n" + specialString +"\n");
            for(int i = 0; i < res.length; i++){
                res[i] = destuff(fileName);
            }
            fis.close();
            return null;
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return res;
    }

    public static boolean logEvent(String event) {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss").format(Calendar.getInstance().getTime());
        try {
            File f = new File(Configuration.LOG_FILE);
            if (f.isDirectory()) {
                return false;
            }
            if (!f.exists()) {
                f.createNewFile();
            }
            PrintWriter pw = new PrintWriter(f);
            pw.print("\n" + specialString + "\n" + stuff(timeStamp + ": " + event));
            pw.close();
            return true;
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return false;
    }
}
