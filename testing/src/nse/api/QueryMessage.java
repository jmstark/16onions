/*
 * Copyright (C) 2016 totakura
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
package nse.api;

import java.nio.ByteBuffer;
import protocol.Protocol;

/**
 * The NSE query message.
 *
 * Used to ask NSE to provide its current message
 *
 * @author totakura
 */
public class QueryMessage extends ApiMessage {
    public QueryMessage() {
        super();
        this.addHeader(Protocol.MessageType.API_NSE_QUERY);
    }

    public void send(ByteBuffer out) {
        super.send(out);
    }

    public static QueryMessage parse(ByteBuffer buf) {
        return new QueryMessage();
    }
}
