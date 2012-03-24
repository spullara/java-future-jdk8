package spullara.util.concurrent;

import java.util.concurrent.Future;

/**
 * A Future that can be set externally.
 */
public interface SettableFuture<T> extends Future<T> {
    void set(T value);
    void setException(Throwable throwable);

}
