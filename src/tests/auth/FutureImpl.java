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
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
class FutureImpl<V, A, B> implements Future {

    private CompletionHandler<V, A> handler;
    private V result;
    private ExecutionException exp;
    private boolean done;
    private boolean cancelled;
    private final ReentrantLock lock;
    private final Condition condition;
    private final B context;
    private static final Logger LOGGER = Logger.getLogger(
            "tests.auth.FutureImpl");

    FutureImpl(CompletionHandler<V, A> handler, B context) {
        this.handler = handler;
        this.done = false;
        this.lock = new ReentrantLock();
        this.condition = lock.newCondition();
        this.cancelled = false;
        this.context = context;
    }

    B getContext() {
        return this.context;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (done) {
            return false;
        }
        this.cancelled = true;
        this.done = true;
        return true;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    private Object getResult() throws ExecutionException {
        if (null != result) {
            return result;
        } else {
            throw exp;
        }
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        if (cancelled) {
            throw new CancellationException();
        }
        if (done) {
            return getResult();
        }
        lock.lockInterruptibly();
        try {
            condition.await();
        } finally {
            lock.unlock();
        }
        return getResult();
    }

    @Override
    public Object get(long arg0, TimeUnit arg1) throws InterruptedException,
            ExecutionException, TimeoutException {
        if (cancelled) {
            throw new CancellationException();
        }
        if (done) {
            return getResult();
        }
        lock.lockInterruptibly();
        try {
            LOGGER.log(Level.FINEST, "Waiting for future's result");
            condition.await(arg0, arg1);
        } finally {
            lock.unlock();
        }
        return getResult();
    }

    void trigger(V result, A attachment) {
        if (this.cancelled) {
            return;
        }
        LOGGER.log(Level.FINEST, "Triggering future");
        this.result = result;
        this.done = true;
        this.handler.completed(result, attachment);
        lock.lock();
        try {
            condition.signal();
        } finally {
            lock.unlock();
        }
    }

    void triggerException(ExecutionException exp, A attachment) {
        if (this.cancelled) {
            return;
        }
        LOGGER.log(Level.FINEST, "Triggering exception for future");
        this.exp = exp;
        this.done = true;
        this.handler.failed(exp, attachment);
        lock.lock();
        try {
            condition.signal();
        } finally {
            lock.unlock();
        }
    }
}
