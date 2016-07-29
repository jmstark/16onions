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
import protocol.Message;
import protocol.MessageParserException;
import protocol.Protocol;

/**
 * The NSE estimate message.
 *
 * This is sent by the NSE module as a response to the query message
 *
 * @author totakura
 */
public class EstimateMessage extends ApiMessage {

    private long estimate;
    private long deviation;

    public EstimateMessage(long estimate, long deviation) {
        assert (estimate <= Integer.MAX_VALUE);
        assert (deviation <= Integer.MAX_VALUE);
        this.addHeader(Protocol.MessageType.API_NSE_ESTIMATE);
        this.estimate = estimate;
        this.deviation = deviation;
        this.size += 8; // 4: estimate + 4:deviation
    }

    public int getEstimate() {
        return (int) estimate;
    }

    public int getDeviation() {
        return (int) deviation;
    }

    @Override
    public void send(ByteBuffer out) {
        out.putInt((int) this.estimate);
        out.putInt((int) this.deviation);
    }

    public static EstimateMessage parse(ByteBuffer buf)
            throws MessageParserException {
        long estimate;
        long deviation;

        if (buf.remaining() != 8) {
            throw new MessageParserException("Invalid size for NSE EstimateMessage");
        }
        estimate = Message.unsignedLongFromInt(buf.getInt());
        deviation = Message.unsignedLongFromInt(buf.getInt());
        return new EstimateMessage(estimate, deviation);
    }
}
