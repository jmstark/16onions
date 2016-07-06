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
package gossip.api;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import protocol.Message;
import protocol.MessageParserException;
import protocol.Protocol;

/**
 *
 * @author totakura
 */
public class ValidationMessage extends ApiMessage {

    private int msgId;
    private boolean valid;

    public ValidationMessage(int msgId, boolean valid) {
        this.addHeader(Protocol.MessageType.API_GOSSIP_VALIDATION);
        this.msgId = msgId;
        this.valid = valid;
        this.size += 4;
    }

    public int getMsgId() {
        return msgId;
    }

    public boolean isValid() {
        return valid;
    }

    @Override
    public void send(ByteBuffer out) {
        super.send(out);
        out.putShort((short) msgId);
        out.putShort((short) (valid ? 1 : 0));
    }

    static ValidationMessage parse(ByteBuffer buf)
            throws MessageParserException {
        ValidationMessage message;
        int msgId;
        boolean valid;

        try {
            msgId = Message.getUnsignedShort(buf);
            valid = (1 == Message.getUnsignedShort(buf));
            message = new ValidationMessage(msgId, valid);
        } catch (BufferUnderflowException | IllegalArgumentException exp) {
            throw new MessageParserException();
        }
        return message;
    }
}
