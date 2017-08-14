package com.voidphone.onion;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RoundScheduler {
	private final ScheduledThreadPoolExecutor scheduler;
	private final long roundtime;

	public RoundScheduler() {
		scheduler = new ScheduledThreadPoolExecutor(1);
		roundtime = Main.getConfig().roundtime;
	}

	public void scheduleInNextRound(Runnable runnable) {
		scheduler.schedule(runnable, elapsedTimeSinceLastRound(), TimeUnit.MILLISECONDS);
	}

	public void scheduleInFutureRounds(Runnable runnable) {
		scheduler.scheduleAtFixedRate(runnable, elapsedTimeSinceLastRound(), roundtime, TimeUnit.MILLISECONDS);
	}

	private long elapsedTimeSinceLastRound() {
		return System.currentTimeMillis() % roundtime;
	}
}
