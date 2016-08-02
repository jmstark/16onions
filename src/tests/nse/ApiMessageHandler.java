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
package tests.nse;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import nse.api.EstimateMessage;
import protocol.MessageHandler;
import protocol.MessageParserException;
import protocol.Protocol;
import protocol.ProtocolException;

/**
 *
 * @author totakura
 */
class ApiMessageHandler extends MessageHandler<Context> {

    private static final Logger LOGGER = Main.getLogger();

    ApiMessageHandler(Context context) {
        super(context);
    }

    @Override
    public void parseMessage(ByteBuffer buf,
            Protocol.MessageType type,
            Context context) throws MessageParserException, ProtocolException {
        int tally;

        if (type != Protocol.MessageType.API_NSE_ESTIMATE) {
            throw new ProtocolException("Received an invalid message");
        }
        //Parse and handle the estimate message
        EstimateMessage message = EstimateMessage.parse(buf);
        context.receivedEstimate();
        tally = context.tally();
        if (tally > 0) {
            LOGGER.log(Level.WARNING,
                    "More estimates are being received than requested");
        }
        LOGGER.log(Level.INFO,
                "Received estimate #peers: {0}; standard deviation of estimate:{1}",
                new Object[]{message.getEstimate(), message.getDeviation()});

    }

}
