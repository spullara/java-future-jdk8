package spullara.util.concurrent;

import spullara.util.functions.Block;
import spullara.util.functions.Blocks;
import spullara.util.functions.Mapper;
import spullara.util.functions.Pair;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * You can use a Promise like an asychronous callback or you can block
 * on it like you would a Future.
 * <p/>
 * Loosely based on: http://twitter.github.com/scala_school/finagle.html
 */
public class Promise<T> implements SettableFuture<T> {

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

    /**
     * Satisfy the Promise with a successful value. This executes all the Blocks associated
     * with "success" in the order they were added to the Promise. We synchronize against
     * another thread adding more success Blocks.
     */
    public void set(T value) {
        if (set.tryAcquire()) {
            successful = true;
            this.value = value;
            read.countDown();
            Block<T> localSuccess = null;
            synchronized (this) {
                if (success != null) {
                    localSuccess = success;
                    success = null;
                }
            }
            if (localSuccess != null) {
                localSuccess.apply(value);
            }
        }
    }

    /**
     * Satisfy the Promise with a failure throwable. This executes all the Blocks associated
     * with "failure" in the order they were added to the Promise. We synchronize against
     * another thread adding more failure Blocks.
     */
    public void setException(Throwable throwable) {
        if (set.tryAcquire()) {
            if (throwable == null) {
                throw new NullPointerException("Throwable cannot be null");
            }
            this.throwable = throwable;
            read.countDown();
            Block<Throwable> localFailed = null;
            synchronized (this) {
                if (failed != null) {
                    localFailed = failed;
                    failed = null;
                }
            }
            if (localFailed != null) {
                localFailed.apply(throwable);
            }
        }
    }

    private Set<Promise> linked;
    private Block<Throwable> raise;
    private Block<Throwable> failed;

    /**
     * Block that is executed when this Promise succeeds.
     */
    private Block<T> success;

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

    @Override
    public void done() {
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
                throw new ExecutionException(throwable);
            }
        } else {
            throw new TimeoutException();
        }
    }

    /**
     * This adds an additional Block that is executed when success occurs if the Promise
     * is not yet satisfied. If it was satisifed successfully we immediately call the Block
     * with the resulting value.
     */
    private void addSuccess(Block<T> block) {
        synchronized (this) {
            if (read.getCount() == 0) {
                if (successful) {
                    block.apply(value);
                }
            } else if (success == null) {
                success = block;
            } else {
                success = Blocks.chain(success, block);
            }
        }
    }

    /**
     * This adds an additional Block that is executed when failure occurs if the Promise
     * is not yet satisfied. If it has already failed we immediately call the Block with
     * the resulting throwable.
     */
    private void addFailure(Block<Throwable> block) {
        synchronized (this) {
            if (read.getCount() == 0) {
                if (!successful) {
                    block.apply(throwable);
                }
            } else if (failed == null) {
                failed = block;
            } else {
                failed = Blocks.chain(failed, block);
            }
        }
    }

    /**
     * Return a Promise that holds the resulting Mapper type. When the underlying Promise completes
     * successfully, map the value using the mapper and satisfied the returned Promise. If the
     * underlying Promise fails, the return promise fails.
     */

    public <V> Promise<V> map(final Mapper<T, V> mapper) {
        final Promise<V> promise = new Promise<V>();
        promise.link(this);
        addSuccess(new Block<T>() {
            public void apply(T value) {
                promise.set(mapper.map(value));
            }
        });
        addFailure(new Block<Throwable>() {
            public void apply(Throwable throwable) {
                promise.setException(throwable);
            }
        });
        return promise;
    }

    private <V> void link(Promise<V> promise) {
        synchronized (this) {
            if (linked == null) {
                linked = new HashSet();
            }
            linked.add(promise);
        }
    }

    public <V> Promise<V> flatMap(final Mapper<T, Promise<V>> mapper) {
        final Promise<V> promise = new Promise<V>();
        promise.link(this);
        addSuccess(new Block<T>() {
            public void apply(T value) {
                Promise<V> mapped = mapper.map(value);
                mapped.addSuccess(new Block<V>() {
                    public void apply(V v) {
                        promise.set(v);
                    }
                });
                mapped.addFailure(new Block<Throwable>() {
                    public void apply(Throwable throwable) {
                        promise.setException(throwable);
                    }
                });
            }
        });
        addFailure(new Block<Throwable>() {
            public void apply(Throwable throwable) {
                promise.setException(throwable);
            }
        });
        return promise;
    }

    public <B> Promise<Pair<T, B>> join(Promise<B> promiseB) {
        final Promise<Pair<T, B>> promise = new Promise<Pair<T, B>>();
        promise.link(this);
        promise.link(promiseB);
        final AtomicReference ref = new AtomicReference();
        addSuccess(new Block<T>() {
            @Override
            public void apply(T value) {
                if (!ref.weakCompareAndSet(null, value)) {
                    promise.set(new Pair(value, (B) ref.get()));
                }
            }
        });
        promiseB.addSuccess(new Block<B>() {
            @Override
            public void apply(B value) {
                if (!ref.weakCompareAndSet(null, value)) {
                    promise.set(new Pair((T) ref.get(), value));
                }
            }
        });
        addFailure(new Block<Throwable>() {
            @Override
            public void apply(Throwable throwable) {
                promise.setException(throwable);
            }
        });
        promiseB.addFailure(new Block<Throwable>() {
            @Override
            public void apply(Throwable throwable) {
                promise.setException(throwable);
            }
        });
        return promise;
    }

    public Promise<T> select(Promise<T> promiseB) {
        final Promise<T> promise = new Promise<T>();
        promise.link(this);
        promise.link(promiseB);
        final AtomicBoolean done = new AtomicBoolean();
        final AtomicBoolean failed = new AtomicBoolean();
        Block<T> success = new Block<T>() {
            @Override
            public void apply(T t) {
                if (done.compareAndSet(false, true)) {
                    promise.set(t);
                }
            }
        };
        Block<Throwable> fail = new Block<Throwable>() {
            @Override
            public void apply(Throwable t) {
                if (!failed.compareAndSet(false, true)) {
                    promise.setException(t);
                }
            }
        };
        addSuccess(success);
        promiseB.addSuccess(success);
        addFailure(fail);
        promiseB.addFailure(fail);
        return promise;
    }

    public void foreach(Block<T> block) {
        addSuccess(block);
    }

    /**
     * If and only if the Promise succeeds, execute this Block. If this method is called
     * after the Promise has already succeeded the Block is executed immediately.
     */
    public Promise<T> onSuccess(Block<T> block) {
        addSuccess(block);
        return this;
    }

    /**
     * If and only if the Promise fails, execute this Block. If this method is called
     * after the Promise has already failed the Block is executed immediately.
     */
    public Promise<T> onFailure(Block<Throwable> block) {
        addFailure(block);
        return this;
    }

    public Promise<T> onRaise(Block<Throwable> block) {
        synchronized (this) {
            if (raise == null) {
                raise = block;
            } else {
                raise = Blocks.chain(raise, block);
            }
        }
        return this;
    }

    public Promise<T> ensure(final Runnable runnable) {
        addSuccess(new Block<T>() {
            public void apply(T t) {
                runnable.run();
            }
        });
        addFailure(new Block<Throwable>() {
            public void apply(Throwable throwable) {
                runnable.run();
            }
        });
        return this;
    }

    public Promise<T> rescue(final Mapper<Throwable, T> mapper) {
        final Promise<T> promise = new Promise<T>();
        promise.link(this);
        addSuccess(new Block<T>() {
            public void apply(T t) {
                promise.set(t);
            }
        });
        addFailure(new Block<Throwable>() {
            public void apply(Throwable throwable) {
                promise.set(mapper.map(throwable));
            }
        });
        return promise;
    }

    public void raise(Throwable e) {
        synchronized (this) {
            if (set.availablePermits() == 1) {
                if (raise != null) {
                    raise.apply(e);
                }
            }
            if (linked != null) {
                for (Promise promise : linked) {
                    promise.raise(e);
                }
            }
        }
    }
}
