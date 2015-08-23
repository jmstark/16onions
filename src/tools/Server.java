/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

/**
 *
 * @author Emertat
 */
public interface Server {
    public void handleMessage(String message, int port);
}
