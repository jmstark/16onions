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
package tools;

import java.io.IOException;
import static java.lang.Math.max;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import org.apache.commons.cli.CommandLine;
import tools.config.CliParser;
import java.util.logging.Logger;
import static java.lang.Math.max;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public abstract class Program {

    protected final String name;
    protected final String description;
    protected ScheduledExecutorService scheduledExecutor;
    protected AsynchronousChannelGroup group;
    protected static Logger LOGGER;
    private final AtomicBoolean inShutdown = new AtomicBoolean();
    private Thread shutdownThread;

    protected Program(String name, String description) {
        this.name = name;
        this.description = description;
        LOGGER = Logger.getLogger(name);
    }

    protected void addParserOptions(CliParser parser) {
    }

    abstract protected void parseCommandLine(CommandLine cli, CliParser parser);

    abstract protected void cleanup();

    abstract protected void run();

    protected boolean inShutdown() {
        return inShutdown.get();
    }

    private void await() {
        boolean terminated;

        shutdownThread = new Thread() {
            @Override
            public void run() {
                if (inShutdown.get()) {
                    return;
                }
                LOGGER.log(Level.INFO,
                        "Shutting down; this may take a while...");
                shutdown();
            }
        };

        Runtime.getRuntime().addShutdownHook(shutdownThread);
        do {
            try {
                terminated = group.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                break;
            }
            if (terminated) {
                break;
            }
        } while (true);
    }

    protected void shutdown() {
        // stop recursive/futhur calls to shutdown if we are already shutting down
        if (!inShutdown.compareAndSet(false, true)) {
            return;
        }
        cleanup();
        LOGGER.fine("shutting down...");
        group.shutdown();
    }

    public void start(String args[]) throws IOException {
        CliParser parser;
        parser = new CliParser(name, description);
        addParserOptions(parser);

        CommandLine cli = parser.parse(args);
        parseCommandLine(cli, parser);

        final int cores = Runtime.getRuntime().availableProcessors();
        final ThreadFactory threadFactory = Executors.defaultThreadFactory();
        group = AsynchronousChannelGroup.withFixedThreadPool(
                max(1, cores - 1), threadFactory);
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(
                threadFactory);
        run();
        await();
    }
}
