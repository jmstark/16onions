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

import gossip.Cache;
import java.nio.ByteBuffer;
import protocol.MessageHandler;
import protocol.MessageParserException;
import protocol.Protocol.MessageType;
import protocol.ProtocolException;

/**
 *
 * @author totakura
 */
class ApiMessageHandler extends MessageHandler<ClientContext> {

    private final Cache cache;

    ApiMessageHandler(ClientContext context, Cache cache) {
        super(context);
        this.cache = cache;
    }

    @Override
    public void parseMessage(ByteBuffer buf,
            MessageType type,
            ClientContext context) throws
            MessageParserException, ProtocolException {
        ApiMessage message;

        message = dispatch(buf, type);
        handleMessage(message, type, context);
    }

    private ApiMessage dispatch(ByteBuffer buf, MessageType type)
            throws MessageParserException {
        switch (type) {
            case API_GOSSIP_ANNOUNCE:
                return AnnounceMessage.parse(buf);
            case API_GOSSIP_NOTIFY:
                throw new UnsupportedOperationException("Not supported yet.");
            //return NotifyMessage.parse(buf);
            default:
                throw new MessageParserException("Unknown message");
        }
    }

    private void handleMessage(ApiMessage message,
            MessageType type, ClientContext context) {
        switch (type) {
            case API_GOSSIP_ANNOUNCE:
                throw new UnsupportedOperationException("Not supported yet.");
            case API_GOSSIP_NOTIFY:
                throw new UnsupportedOperationException("Not supported yet.");
        }
    }

}
