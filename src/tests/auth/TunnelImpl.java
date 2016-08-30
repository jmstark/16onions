/*
 * Copyright (C) 2016 Sree Harsha Totakura <sreeharsha@totakura.in>
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
package tests.auth;

import java.nio.channels.CompletionHandler;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.Future;
import onionauth.api.OnionAuthDecrypt;
import onionauth.api.OnionAuthEncrypt;
import protocol.Connection;
import protocol.MessageSizeExceededException;

public class TunnelImpl implements Tunnel {

    private final LinkedList<Session> sessions;
    private final Session main;
    private final Connection connection;
    private static final Map<Integer, FutureImpl> requestMap = new HashMap(3000);
    private static int counter = 0;

    public TunnelImpl(Session session, Connection connection) {
        this.main = session;
        this.sessions = new LinkedList();
        this.connection = connection;
    }

    public static FutureImpl getFuture(int id) throws NoSuchElementException {
        FutureImpl future = requestMap.remove(id);
        if (null == future) {
            throw new NoSuchElementException(Integer.toString(id));
        }
        return future;
    }

    @Override
    public void addHop(Session session) {
        this.sessions.addLast(session);
    }

    @Override
    public boolean removeHop(Session session) {
        return this.sessions.remove(session);
    }

    private long[] getSessionIDs() {
        sessions.addFirst(main);
        long[] ids = new long[sessions.size()];
        int index = 0;
        for (Session session : sessions) {
            ids[index] = session.getID();
        }
        sessions.removeFirst();
        return ids;
    }

    @Override
    public Future<byte[]> encrypt(byte[] payload,
            CompletionHandler<byte[], ? extends Object> handler) throws
            MessageSizeExceededException {
        OnionAuthEncrypt request;
        int requestID;

        requestID = counter++;
        long[] ids = getSessionIDs();
        request = new OnionAuthEncrypt(requestID, ids, payload);
        FutureImpl future = new FutureImpl(handler, null);
        this.requestMap.put(requestID, future);
        connection.sendMsg(request);
        return future;
    }

    @Override
    public Future<byte[]> decrypt(byte[] payload,
            CompletionHandler<byte[], ? extends Object> handler) throws
            MessageSizeExceededException {

        OnionAuthDecrypt request;
        int requestID;
        long[] ids;

        requestID = counter++;
        ids = getSessionIDs();
        request = new OnionAuthDecrypt(requestID, ids, payload);
        FutureImpl future = new FutureImpl(handler, null);
        this.requestMap.put(requestID, future);
        connection.sendMsg(request);
        return future;
    }

}
