/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package protocol;

import java.io.File;
import util.MyRandom;

/**
 *
 * @author Emertat
 */
public class HostKey {

    File f;

    public HostKey(String fileName) {
        f = new File(fileName);
    }

    public String getPsuedoIdentity() {
        //TODO: this function returns random value for now.
        return new MyRandom().randString(Protocol.IDENTITY_LENGTH);
    }
}
