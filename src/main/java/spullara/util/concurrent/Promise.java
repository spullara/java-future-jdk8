package spullara.util.concurrent;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * You can use a Promise like an asynchronous callback or you can block
 * on it like you would a Future. Callbacks are executed:
 * 1. In the thread that sets the promise, if it wasn't satisfied when
 * the callback was added.
 * 2. In the caller thread, if it was satisfied when the callback is added.
 * You should never block in the callback. If you need to do more work that does
 * block you should again use a Promise and flatMap.
 * <p/>
 * Loosely based on: http://twitter.github.com/scala_school/finagle.html
 */
public class Promise<T> implements SettableFuture<T> {

    /**
     * Create an unsatisfied Promise.
     */
    public Promise() {
    }

    /**
     * Create a Promise already satisfied with a successful value.
     *
     * @param t The value of the Promise.
     */
    public Promise(T t) {
        set(t);
    }

    /**
     * Create a Promise already satisfied with a failed throwable.
     */
    public Promise(Throwable t) {
        setException(t);
    }

    /**
     * This semaphore guards whether or not a Promise has already been set.
     */
    private final Semaphore set = new Semaphore(1);

    /**
     * This latch is counted down when the Promise can be read.
     */
    private final CountDownLatch read = new CountDownLatch(1);

    /**
     * Cancelled
     */
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    /**
     * The value of a successful Promise.
     */
    private T value;

    /**
     * The throwable of a failed Promise.
     */
    private Throwable throwable;

    /**
     * Successful
     */
    private volatile boolean successful = false;

    public static <T> Promise<T> execute(ExecutorService es, Callable<T> callable) {
        Promise<T> promise = new Promise<>();
        es.submit(() -> {
            try {
                if (!promise.isCancelled()) {
                    promise.set(callable.call());
                }
            } catch (Throwable th) {
                promise.setException(th);
            }
        });
        return promise;
    }

    public static <T> Promise<T> execute(Callable<T> callable) {
        Promise<T> promise = new Promise<>();
        ForkJoinPool.commonPool().submit(() -> {
            try {
                if (!promise.isCancelled()) {
                    promise.set(callable.call());
                }
            } catch (Throwable th) {
                promise.setException(th);
            }
        });
        return promise;
    }

    @SafeVarargs
    public static <T> Promise<? extends List<T>> collect(Promise<T>... promises) {
        Promise<List<T>> promiseOfList = new Promise<>();
        int size = promises.length;
        if (size == 0) {
            promiseOfList.set(Collections.emptyList());
        } else {
            List<T> list = Collections.synchronizedList(new ArrayList<>(size));
            for (Promise<T> promise : promises) {
                promise.onSuccess(v -> {
                    list.add(v);
                    if (list.size() == size) {
                        promiseOfList.set(list);
                    }
                }).onFailure(promiseOfList::setException);
            }
        }
        return promiseOfList;
    }

    @SafeVarargs
    public static <T> Promise<T> select(Promise<T>... promises) {
        AtomicInteger ai = new AtomicInteger(promises.length);
        AtomicBoolean done = new AtomicBoolean();
        Promise<T> promise = new Promise<>();
        for (Promise<T> p : promises) {
            p.onSuccess(t -> {
                if (done.compareAndSet(false, true)) {
                    promise.set(t);
                }
            });
            p.onFailure(th -> {
                if (ai.decrementAndGet() == 0) {
                    promise.setException(th);
                }
            });
        }
        return promise;
    }

    /**
     * Satisfy the Promise with a successful value. This executes all the Consumers associated
     * with "success" in the order they were added to the Promise. We synchronize against
     * another thread adding more success Consumers.
     */
    public void set(T value) {
        if (set.tryAcquire()) {
            Consumer<T> localSuccess;
            synchronized (this) {
                successful = true;
                this.value = value;
                if (success != null) {
                    localSuccess = success;
                    success = null;
                } else {
                    localSuccess = null;
                }
                read.countDown();
            }
            if (localSuccess != null) {
                localSuccess.accept(value);
            }
            done();
        }
    }

    /**
     * Satisfy the Promise with a failure throwable. This executes all the Consumers associated
     * with "failure" in the order they were added to the Promise. We synchronize against
     * another thread adding more failure Consumers.
     */
    public void setException(Throwable throwable) {
        if (set.tryAcquire()) {
            this.throwable = throwable;
            read.countDown();
            Consumer<Throwable> localFailed = null;
            synchronized (this) {
                if (failed != null) {
                    localFailed = failed;
                    failed = null;
                }
            }
            if (localFailed != null) {
                localFailed.accept(throwable);
            }
            done();
        }
    }

    @Override
    public void done() {
    }

    /**
     * Linked Promises recieve the same signals their linker recieves.
     */
    private Set<Promise> linked;

    /**
     * Consumer that is executed when a signal is raised on this Promise.
     */
    private Consumer<Throwable> raise;

    /**
     * Consumer that is executed when this Promise fails.
     */
    private Consumer<Throwable> failed;

    /**
     * Consumer that is executed when this Promise succeeds.
     */
    private Consumer<T> success;

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (cancelled.compareAndSet(false, true)) {
            CancellationException cancel = new CancellationException();
            raise(cancel);
            setException(cancel);
            return true;
        }
        return false;
    }

    @Override
    public boolean isCancelled() {
        return cancelled.get();
    }

    @Override
    public boolean isDone() {
        return set.availablePermits() == 0;
    }

    /**
     * Wait until the Promise is satisfied. If it was successful, return the
     * value. If it fails, throw an ExecutionException with the failure throwable
     * as the cause. It can be interrupted.
     */
    public T get() throws InterruptedException, ExecutionException {
        read.await();
        if (throwable == null) {
            return value;
        } else {
            if (throwable instanceof CancellationException) {
                throw (CancellationException) throwable;
            }
            throw new ExecutionException(throwable);
        }
    }

    /**
     * Wait until the Promise is satisfied or timeout. If it was successful, return
     * the value. If it fails, throw an ExecutionException with the failure throwable
     * as the cause. Otherwise, throw a TimeoutException. It can be interrupted.
     */
    public T get(long timeout, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        if (read.await(timeout, timeUnit)) {
            if (throwable == null) {
                return value;
            } else {
                if (throwable instanceof CancellationException) {
                    throw (CancellationException) throwable;
                }
                throw new ExecutionException(throwable);
            }
        } else {
            throw new TimeoutException();
        }
    }

    /**
     * This adds an additional Consumer that is executed when success occurs if the Promise
     * is not yet satisfied. If it was satisifed successfully we immediately call the Consumer
     * with the resulting value.
     */
    private void addSuccess(Consumer<T> block) {
        synchronized (this) {
            if (read.getCount() == 0) {
                if (successful) {
                    block.accept(value);
                }
            } else if (success == null) {
                success = block;
            } else {
                success = success.andThen(block);
            }
        }
    }

    /**
     * This adds an additional Consumer that is executed when failure occurs if the Promise
     * is not yet satisfied. If it has already failed we immediately call the Consumer with
     * the resulting throwable.
     */
    private void addFailure(Consumer<Throwable> block) {
        synchronized (this) {
            if (read.getCount() == 0) {
                if (!successful) {
                    block.accept(throwable);
                }
            } else if (failed == null) {
                failed = block;
            } else {
                failed = failed.andThen(block);
            }
        }
    }

    /**
     * Return a Promise that holds the resulting Mapper type. When the underlying Promise completes
     * successfully, map the value using the mapper and satisfied the returned Promise. If the
     * underlying Promise fails, the return promise fails.
     */
    public <V> Promise<V> map(Function<T, V> mapper) {
        Promise<V> promise = new Promise<>();
        promise.link(this);
        addSuccess(v -> promise.set(mapper.apply(v)));
        addFailure(promise::setException);
        return promise;
    }

    /**
     * Return a Promise of the resulting Promise type. When the underlying Promise completes
     * successfully call the mapper with the value and connect the resulting Promise to the
     * returned promise. If either the underlying Promise or the mapped Promise fails, the
     * return Promise fails.
     */
    public <V> Promise<V> flatMap(Function<T, Promise<V>> mapper) {
        Promise<V> promise = new Promise<>();
        promise.link(this);
        addSuccess(value -> {
            Promise<V> mapped = mapper.apply(value);
            promise.link(mapped);
            mapped.addSuccess(promise::set);
            mapped.addFailure(promise::setException);
        });
        addFailure(promise::setException);
        return promise;
    }

    /**
     * Return a Promise that is a Pair type of the underlying Promise and the passed Promise.
     * The returned Promise succeeds if both the Promises succeed. If either Promise fails
     * the returned Promise also fails.
     */
    @SuppressWarnings("unchecked")
    public <B> Promise<Pair<T, B>> join(Promise<B> promiseB) {
        Promise<Pair<T, B>> promise = new Promise<>();
        promise.link(this);
        promise.link(promiseB);
        AtomicReference ref = new AtomicReference();
        addSuccess(v -> {
            if (!ref.weakCompareAndSet(null, v)) {
                promise.set(new Pair<>(v, (B) ref.get()));
            }
        });
        promiseB.addSuccess(v -> {
            if (!ref.weakCompareAndSet(null, v)) {
                promise.set(new Pair<>((T) ref.get(), v));
            }
        });
        addFailure(promise::setException);
        promiseB.addFailure(promise::setException);
        return promise;
    }

    /**
     * Reeturn a Promise of same type as the underlying Promise and the passed Promise. The returned Proimse
     * gets the value of the first one that succeeds or fails if they both fail.
     */
    public Promise<T> select(Promise<T> promiseB) {
        Promise<T> promise = new Promise<>();
        promise.link(this);
        promise.link(promiseB);
        AtomicBoolean done = new AtomicBoolean();
        AtomicBoolean failed = new AtomicBoolean();
        Consumer<T> success = v -> {
            if (done.compareAndSet(false, true)) {
                promise.set(v);
            }
        };
        Consumer<Throwable> fail = e -> {
            if (!failed.compareAndSet(false, true)) {
                promise.setException(e);
            }
        };
        addSuccess(success);
        promiseB.addSuccess(success);
        addFailure(fail);
        promiseB.addFailure(fail);
        return promise;
    }

    /**
     * If and only if the Promise succeeds, execute this Consumer. If this method is called
     * after the Promise has already succeeded the Consumer is executed immediately.
     */
    public Promise<T> onSuccess(Consumer<T> block) {
        addSuccess(block);
        return this;
    }

    /**
     * If and only if the Promise fails, execute this Consumer. If this method is called
     * after the Promise has already failed the Consumer is executed immediately.
     */
    public Promise<T> onFailure(Consumer<Throwable> block) {
        addFailure(block);
        return this;
    }

    /**
     * Always execute the Runnable after the Promise is satisfied. If it has already
     * been satisifed, execute the Runnable immediately.
     */
    public Promise<T> ensure(Runnable runnable) {
        addSuccess(v -> runnable.run());
        addFailure(e -> runnable.run());
        return this;
    }

    /**
     * If the underlying Promise fails, replace the failure with a success by mapping
     * the throwable to a value.
     */
    public Promise<T> rescue(Function<Throwable, T> mapper) {
        Promise<T> promise = new Promise<>();
        promise.link(this);
        addSuccess(promise::set);
        addFailure(e -> promise.set(mapper.apply(e)));
        return promise;
    }

    /**
     * Raise a signal to this Promise and all its linked Promises.
     */
    public void raise(Throwable e) {
        synchronized (this) {
            if (set.availablePermits() == 1) {
                if (raise != null) {
                    raise.accept(e);
                }
            }
            if (linked != null) {
                for (Promise promise : linked) {
                    promise.raise(e);
                }
            }
        }
    }

    /**
     * If there is a signal raised on this Promise after the block is added, execute this Consumer.
     */
    public Promise<T> onRaise(Consumer<Throwable> block) {
        synchronized (this) {
            if (raise == null) {
                raise = block;
            } else {
                raise = raise.andThen(block);
            }
        }
        return this;
    }

    /**
     * Add a Promise to the set of linked Promises that this Promise signals when raised.
     */
    public void link(Promise promise) {
        synchronized (this) {
            if (linked == null) {
                linked = new HashSet<>();
            }
            linked.add(promise);
        }
    }

}
