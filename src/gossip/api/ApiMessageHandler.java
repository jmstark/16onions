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
import protocol.Protocol;
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
            Protocol.MessageType type,
            ClientContext closure) throws
            MessageParserException, ProtocolException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
