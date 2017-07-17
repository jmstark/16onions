/*
 * Copyright (C) 2017 Sree Harsha Totakura <sreeharsha@totakura.in>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package onionauth.api;

import auth.api.OnionAuthError;
import java.nio.ByteBuffer;
import org.junit.Test;
import static org.junit.Assert.*;
import protocol.MessageParserException;
import protocol.Protocol;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class OnionAuthErrorTest {

    private static long requestID;
    private static final ByteBuffer out = ByteBuffer.allocate(
            Protocol.MAX_MESSAGE_SIZE);
    private final OnionAuthError message;

    public OnionAuthErrorTest() {
        requestID = util.MyRandom.randUInt();
        message = new OnionAuthError(requestID);
    }

    @Test
    public void testParse() throws MessageParserException {
        OnionAuthError result;
        System.out.println("testParse");
        out.clear();
        message.send(out);
        out.flip();
        out.position(4);
        result = OnionAuthError.parse(out);
        assertEquals(result, message);
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}
}
