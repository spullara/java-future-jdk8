package spullara.util.concurrent;

import java.util.functions.*;
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

    <T> Future<T> map(Mapper<V, T> function);
    <T> Future<T> flatMap(Mapper<V, Future<T>> function);
    void foreach(Block<V> function);
    <T> Future<T> andThen(Mapper<V, Future<T>> function);

    Future<V> onFailure(Block<Throwable> function);
    Future<V> onSuccess(Block<V> function);

    Future<V> ensure(Runnable runnable);
    Future<V> rescue(Mapper<Throwable, V> function);

    <A, B> Future<Tuple2<A, B>> join(Future<A> futureA, Future<B> futureB);
    Future<V> select(Future<V> future1, Future<V> future2);
}