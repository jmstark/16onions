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

import protocol.ProtocolException;

/**
 *
 * @author totakura
 */
interface Context {

    /**
     * Call this function whenever a query has been sent
     */
    void sentQuery();

    /**
     * Call this function whenever an estimate has been received
     */
    void receivedEstimate() throws ProtocolException;

    /**
     * Tallies the # sent queries and # of received estimates.
     *
     * 0 if both match; negative if less number of estimates are received;
     * positive if more estimates are received than sent queries
     *
     * @return the tally
     */
    int tally();
}
