/*
 * Copyright (c) 2017, Charlie Groh and Josef Stark. All rights reserved.
 * 
 * This file is part of 16onions.
 *
 * 16onions is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * 16onions is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with 16onions.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.voidphone.general;

public class General {
	private static long debugTimer = 0;
	private static boolean debugInitialized = false;
	private final static boolean debug = true;

	public static void initDebugging() {
		if (debugInitialized) {
			return;
		}
		debugTimer = System.nanoTime();
	}

	public static void debug(String s) {
		if (debug) {
			System.out.println("[" + (System.nanoTime() - debugTimer)
					+ "] DEBUG: " + s);
		}
	}
	
	public static void info(String s) {
		System.out.println("INFO: " + s);
	}
	
	public static void warning(String s) {
		System.err.println("WARNING: " + s);
	}
	
	public static void error(String s) {
		System.err.println("ERROR: " + s);
	}

	public static void fatal(String s) {
		System.err.println("FATAL: " + s);
		System.exit(1);
	}

	public static void fatalException(Exception e) {
		if (debug) {
			e.printStackTrace();
		}
		General.fatal(e.getMessage());
	}
}
