/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package protocol.dht;

import java.util.Arrays;

/**
 *
 * @author totakura
 */
public class DHTContent {

    byte[] value;

    public DHTContent(byte[] value) {
        this.value = value;
    }

    public byte[] getValue() {
        return this.value;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DHTContent other = (DHTContent) obj;
        return Arrays.equals(this.value, other.value);
    }
}
