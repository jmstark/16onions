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
import java.util.concurrent.Future;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public interface Tunnel {
    public void addHop(Session session);

    public void removeHop(Session session);

    public Future<byte[]> encrypt(byte[] payload,
            CompletionHandler<byte[], ? extends Object> handler);

    public Future<byte[]> decrypt(byte[] payload,
            CompletionHandler<byte[], ? extends Object> handler);
}
