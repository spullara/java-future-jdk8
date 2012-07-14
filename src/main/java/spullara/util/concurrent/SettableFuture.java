package spullara.util.concurrent;

import java.util.concurrent.Future;

/**
 * A Future that can be set externally.
 */
public interface SettableFuture<T> extends Future<T> {
    /**
     * Sets the result of this future to the given value unless
     * this future has already been set or has been cancelled.
     *
     * <p>This method is invoked internally by the {@link #run} method
     * upon successful completion of the computation.
     *
     * @param v the value
     */
    void set(T value);

    /**
     * Causes this future to report an {@link java.util.concurrent.ExecutionException}
     * with the given throwable as its cause, unless this future has
     * already been set or has been cancelled.
     *
     * <p>This method is invoked internally by the {@link #run} method
     * upon failure of the computation.
     *
     * @param t the cause of failure
     */
    void setException(Throwable throwable);

    /**
     * Called after all handlers have been completed post finish.
     */
    void done();
}
