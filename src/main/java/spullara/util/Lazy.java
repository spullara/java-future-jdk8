package spullara.util;

import java.util.concurrent.Callable;

/**
 * Value isn't set until you ask for it and is only
 * calculated once.
 *
 * See
 * http://www.cs.umd.edu/~pugh/java/memoryModel/DoubleCheckedLocking.html
 *
 * The section:
 *
 * "Fixing Double-Checked Locking using Volatile"
 *
 * This is also the same pattern that "lazy val" in Scala uses to
 * ensure once-and-only-once delayed initialization.
 */
public class Lazy<T> {
    private volatile boolean set;
    private final Callable<T> callable;
    private T value;

    public Lazy(Callable<T> callable) {
        this.callable = callable;
    }

    public T get() {
        // This access of set requires a memory barrier
        if (!set) {
            // Now we synchronize to have only a single executor
            synchronized (this) {
                // Check again to make sure another thread didn't beat us to the lock
                if (!set) {
                    // We got this.
                    try {
                        // Evaluate the passed lambda
                        value = callable.call();
                        set = true;
                    } catch (Exception e) {
                        throw new RuntimeException("Lazy initialization failure", e);
                    }
                }
            }
        }
        return value;
    }
}
