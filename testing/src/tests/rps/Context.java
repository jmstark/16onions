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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.crypto.digests.SHA256Digest;
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
    private ScheduledFuture<?> future;
    private final SHA256Digest digest;

    public Context(Connection connection,
            ScheduledExecutorService scheduledExecutor,
            Logger logger) {
        super(null);
        this.logger = logger;
        this.connection = connection;
        this.scheduledExecutor = scheduledExecutor;
        this.random = new Random();
        this.future = scheduleNextQuery(5 * 1000); // run query after 5 seconds
        this.digest = new SHA256Digest();
    }

    /**
     * Schedules the next query.
     *
     * @param delay delay in milliseconds after which the next query should be
     * made.
     */
    private ScheduledFuture<?> scheduleNextQuery(int delay) {
        return scheduledExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                RpsQueryMessage query;
                int delay;

                query = new RpsQueryMessage();
                logger.info("Sent a RPS query");
                connection.sendMsg(query);
                delay = random.nextInt(30 * 1000); // 30 seconds
                future = scheduleNextQuery(delay);
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
                byte[] digestOut;
                message = RpsPeerMessage.parse(buf);
                encoding = SecurityHelper.encodeRSAPublicKey(message.
                        getHostkey());
                digest.reset();
                digestOut = new byte[digest.getDigestSize()];
                digest.update(encoding, 0, encoding.length);
                digest.doFinal(digestOut, 0);
                logger.log(Level.INFO,
                        "Received a random peer with address: {0} and PubKey: {1}",
                        new Object[]{message.getAddress(),
                            Base64.
                                    toBase64String(digestOut, 0,
                                            digestOut.length)});
                digest.reset();
                break;
            default:

        }
    }

    void shutdown() {
        this.future.cancel(true);
    }

}
