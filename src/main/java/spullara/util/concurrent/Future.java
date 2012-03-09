package spullara.util.concurrent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;

public interface Future<V> {

    boolean cancel(boolean mayInterruptIfRunning);
    boolean isCancelled();
    boolean isDone();
    V get() throws InterruptedException, ExecutionException;
    V get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException;

    <T> Future<T> map(Function1<V, T> function);
    <T> Future<T> flatMap(Function1<V, Future<T>> function);
    void foreach(Function<V> function);

    Future<V> onFailure(Function<Throwable> function);
    Future<V> onSuccess(Function<V> function);

    Future<V> ensure(Runnable runnable);
    Future<V> rescue(Function1<Throwable, V> function);

    <A, B> Future<Tuple2<A, B>> join(Future<A> futureA, Future<B> futureB);
    Future<V> select(Future<V> future1, Future<V> future2);
}