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

	public static void fatal(String s) {
		System.err.println("FATAL: " + s);
		System.exit(1);
	}

	public static void fatalException(Exception e) {
		if (debug) {
			e.printStackTrace();
		}
		General.fatal("Unexpected Exception!");
	}
}
