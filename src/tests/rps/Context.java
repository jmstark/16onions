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
/**
 * Message handler and querier for RPS
 *
 * @author totakura
 */
package tests.rps;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bouncycastle.util.encoders.Base64;
import protocol.Connection;
import protocol.MessageHandler;
import protocol.MessageParserException;
import protocol.Protocol;
import protocol.ProtocolException;
import rps.api.RpsPeerMessage;
import rps.api.RpsQueryMessage;
import util.SecurityHelper;

class Context extends MessageHandler<Void> {

    private final Logger logger;
    private final ScheduledExecutorService scheduledExecutor;
    private final Connection connection;
    private final Random random;

    public Context(Connection connection,
            ScheduledExecutorService scheduledExecutor,
            Logger logger) {
        super(null);
        this.logger = logger;
        this.connection = connection;
        this.scheduledExecutor = scheduledExecutor;
        this.random = new Random();
        scheduleNextQuery(5 * 1000); // run query after 5 seconds
    }

    /**
     * Schedules the next query.
     *
     * @param delay delay in milliseconds after which the next query should be
     * made.
     */
    private void scheduleNextQuery(int delay) {
        scheduledExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                RpsQueryMessage query;
                int delay;

                query = new RpsQueryMessage();
                logger.info("Sent a RPS query");
                connection.sendMsg(query);
                delay = random.nextInt(30 * 1000); // 30 seconds
                scheduleNextQuery(delay);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public void parseMessage(ByteBuffer buf,
            Protocol.MessageType type, Void closure)
            throws MessageParserException, ProtocolException {
        switch (type) {
            case API_RPS_PEER:
                RpsPeerMessage message;
                byte[] encoding;
                message = RpsPeerMessage.parse(buf);
                encoding = SecurityHelper.encodeRSAPublicKey(message.getHostkey());
                logger.log(Level.INFO,
                        "Received a random peer with address: {0} and PubKey: {1}",
                        new Object[]{message.getAddress(),
                            Base64.toBase64String(encoding, 0, 16)});
                break;
            default:

        }
    }

}
