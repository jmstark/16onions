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

class ContextImpl implements Context {

    private int countQueries;
    private int countEstimates;

    ContextImpl() {
        countQueries = 0;
        countEstimates = 0;
    }

    @Override
    public void sentQuery() {
        countQueries++;
    }

    @Override
    public void receivedEstimate() throws ProtocolException {
        if (countQueries <= countEstimates) {
            throw new ProtocolException("Estimate received without asking for it");
        }
        countEstimates++;
    }

    @Override
    public int tally() {
        return countEstimates - countQueries;
    }
}
