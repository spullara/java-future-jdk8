package spullara.util;

import java.util.concurrent.Callable;

/**
 * Value isn't set until you ask for it and is only
 * calculated once.
 */
public class Lazy<T> {
    private volatile boolean set;
    private final Callable<T> callable;
    private T value;

    public Lazy(Callable<T> callable) {
        this.callable = callable;
    }

    public T get() {
        // This access of set should require a memory barrier
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
