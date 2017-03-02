/**
 *
 */
package com.lafaspot.sled.client;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

/**
 * Future object for async operations.
 *
 * @param <V>
 *            response object
 *
 * @author kraman
 *
 */
public class SledFuture<V> implements Future<V> {

	/** Is this future task done? */
	private final AtomicBoolean isDone = new AtomicBoolean(false);
	/** Holds the failure cause. */
	private final AtomicReference<Exception> causeRef = new AtomicReference<Exception>();
	/** Used to synchronize threads. */
	private final Object lock = new Object();
	/** holds the result object. */
	private final AtomicReference<V> resultRef = new AtomicReference<V>();
	/** Wait interval when the user calls get(). */
	private static final int GET_WAIT_INTERVAL_MILLIS = 1000;

	/** the future object. */
	private final Future future;

	/**
	 * Constructor.
	 *
	 * @param future
	 *            the future object
	 */
	public SledFuture(@Nonnull final Future future) {
		this.future = future;
	}

	/**
	 * Is this Future cancelled? TODO: return return true if cancelled
	 *
	 * @return true if Future was cancelled
	 */
	@Override
	public boolean isCancelled() {
		// TODO
		return false;
	}

	/**
	 * Is the future task complete?
	 *
	 * @return true if task is complete
	 */
	@Override
	public boolean isDone() {
		return isDone.get();
	}

	/**
	 * Invoked when the worker has completed its processing.
	 *
	 * @param result
	 *            the result to be set
	 */
	public void done(@Nonnull final V result) {
		synchronized (lock) {
			if (!isDone.get()) {
				resultRef.set(result);
				isDone.set(true);
			}
			lock.notify();
		}
	}

	/**
	 * Invoked when the worker throws an exception.
	 *
	 * @param cause
	 *            the exception that caused execution to fail
	 */
	public void done(final Exception cause) {
		synchronized (lock) {
			if (!isDone.get()) {
				causeRef.set(cause);
				isDone.set(true);

			}
			lock.notify();
		}
	}

	/**
	 * Synchronously get the result, will hold the thread until the task is
	 * complete.
	 *
	 * @return the result object
	 * @throws InterruptedException
	 *             on failure
	 * @throws ExecutionException
	 *             on failure
	 */
	@Override
	public V get() throws InterruptedException, ExecutionException {
		synchronized (lock) {
			while (!isDone.get()) {
				lock.wait(GET_WAIT_INTERVAL_MILLIS);
			}
			lock.notify();
		}
		if (causeRef.get() != null) {
			throw new ExecutionException(causeRef.get());
		} else if (isCancelled()) {
			throw new CancellationException();
		} else {
			return resultRef.get();
		}
	}

	/**
	 * Synchronously get the result, will hold the thread until the task is
	 * complete or timeout is passed.
	 *
	 * @return the result object
	 * @param timeout
	 *            time to wait before giving up
	 * @param unit
	 *            unit value for timeout
	 * @throws InterruptedException
	 *             on failure
	 * @throws ExecutionException
	 *             on failure
	 */
	@Override
	public V get(final long timeout, @Nonnull final TimeUnit unit) throws InterruptedException, ExecutionException {
		synchronized (lock) {
			if (!isDone.get()) {
				lock.wait(unit.toMillis(timeout));
			}
			lock.notify();
		}
		if (isDone.get()) {
			if (causeRef.get() != null) {
				throw new ExecutionException(causeRef.get());
			} else if (isCancelled()) {
				throw new CancellationException();
			} else {
				return resultRef.get();
			}
		} else {
			throw new RuntimeException("Timeout reached.");
		}
	}

	@Override
	public boolean cancel(final boolean mayInterruptIfRunning) {
		// TODO Auto-generated method stub
		return false;
	}

}
