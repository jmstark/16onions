/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

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
    public String randString(int length) {
        char[] s = new char[length];
        for (int i = 0; i < length; i++) {
            s[i] = (char) rand.nextInt(256);
        }
        return new String(s);
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
