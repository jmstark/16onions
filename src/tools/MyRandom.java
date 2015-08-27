/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 *
 * @author Emertat
 */
public class MyRandom {

    Random rand = new Random();

    /**
     * This is a tool that generates a random ASCII string with the requested
     * length.
     *
     * @param length
     * @return
     */
    public String randString(int length){
        return new String (randBytes(length), StandardCharsets.US_ASCII);
    }
    
    public byte[] randBytes(int length) {
        byte[] bytes = new byte[length];
        rand.nextBytes(bytes);
        return bytes;
    }  

    public int randInt(int min, int max) {

        int randomNum = rand.nextInt((max - min) + 1) + min;
        return randomNum;
    }
    
    public String randLetter(int length){
        char[] s = new char[length];
        for (int i = 0; i < s.length; i++) {
            s[i] = (char) randInt(65, 86);
        }
        return new String(s);
    }
}
