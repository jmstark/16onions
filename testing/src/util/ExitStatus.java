/*
 * Copyright (C) 2017 Sree Harsha Totakura <sreeharsha@totakura.in>
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
package util;

import lombok.Getter;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public abstract class ExitStatus {
    /**
     * Status code for normal exit
     */
    public static final int OK = 0;

    /**
     * Exiting due to an error while reading configuration
     */
    public static final int CONF_ERROR = -1;

    /**
     * Exiting due to a test failure
     */
    public static final int TEST_FAILURE = 1;
}
