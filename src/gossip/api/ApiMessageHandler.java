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

import gossip.Bus;
import gossip.Cache;
import gossip.Item;
import gossip.p2p.Page;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    private static final Bus BUS = Bus.getInstance();
    private static final Logger LOGGER = Logger.getLogger("Gossip.API");

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
                return NotifyMessage.parse(buf);
            case API_GOSSIP_VALIDATION:
                return ValidationMessage.parse(buf);
            default:
                LOGGER.log(Level.WARNING, "Unknown message received");
                throw new MessageParserException("Unknown message");
        }
    }

    private void handleMessage(ApiMessage message,
            MessageType type, ClientContext context) {
        switch (type) {
            case API_GOSSIP_ANNOUNCE:
                Item older;
                LOGGER.log(Level.FINE, "Processing AnnounceMessage");
                AnnounceMessage announce = (AnnounceMessage) message;
                Page page = new Page(announce.getDatatype(),
                        announce.getData());
                older = cache.addItem(page);
                if (null != older) {
                    LOGGER.warning("Ignoring new announce as a similar message is already in the cache");
                } else {
                    LOGGER.fine("Added a new announce message to cache; it will spread shortly");
                }
                break;
            case API_GOSSIP_NOTIFY:
                LOGGER.log(Level.FINE, "Processing NotifyMessage");
                NotifyMessage notify = (NotifyMessage) message;
                context.addInterest(notify.getDatatype());
                break;
            case API_GOSSIP_VALIDATION:
                LOGGER.log(Level.FINE, "Processing ValidationMessage");
                ValidationMessage validation = (ValidationMessage) message;
                int id = validation.getMsgId();
                Item item = context.findItem(id);
                if (null != item) {
                    cache.markValid(item);
                }
                return;
            default:
                assert (false);
        }
    }

}
