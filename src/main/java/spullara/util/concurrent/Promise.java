package spullara.util.concurrent;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.functions.*;

/**
 * You can use a Promise like an asychronous callback or you can block
 * on it like you would a Future.
 * <p/>
 * Loosely based on: http://twitter.github.com/scala_school/finagle.html
 */
public class Promise<T> {

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
    private Semaphore set = new Semaphore(1);

    /**
     * This latch is counted down when the Promise can be read.
     */
    private CountDownLatch read = new CountDownLatch(1);

    /**
     * The value of a successful Promise.
     */
    private T value;
    
    /**
     * The throwable of a failed Promise.
     */
    private Throwable throwable;

    /**
     * Satisfy the Promise with a successful value. This executes all the Blocks associated
     * with "success" in the order they were added to the Promise. We synchronize against
     * another thread adding more success Blocks.
     */
    public void set(T value) {
        if (set.tryAcquire()) {
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

    /**
     * Linked Promises recieve the same signals their linker recieves.
     */
    private Set<Promise> linked;

    /**
     * Block that is executed when a signal is raised on this Promise.
     */
    private Block<Throwable> raise;

    /**
     * Block that is executed when this Promise fails.
     */
    private Block<Throwable> failed;

    /**
     * Block that is executed when this Promise succeeds.
     */
    private Block<T> success;

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
                if (value != null) {
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
                if (throwable != null) {
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
    public <V> Promise<V> map(Mapper<T, V> mapper) {
        Promise<V> promise = new Promise<V>();
        promise.link(this);
        addSuccess(v -> promise.set(mapper.map(v)));
        addFailure(e -> promise.setException(e));
        return promise;
    }

    /**
     * Return a Promise of the resulting Promise type. When the underlying Promise completes
     * successfully call the mapper with the value and connect the resulting Promise to the
     * returned promise. If either the underlying Promise or the mapped Promise fails, the
     * return Promise fails.
     */
    public <V> Promise<V> flatMap(Mapper<T, Promise<V>> mapper) {
        Promise<V> promise = new Promise<V>();
        promise.link(this);
        addSuccess(value - > {
                        Promise <V> mapped = mapper.map(value);
			promise.link(mapped);
                        mapped.addSuccess(v -> promise.set(v));
                        mapped.addFailure(e - > promise.setException(e));
        });
        addFailure(e - > promise.setException(e));
        return promise;
    }

    /**
     * Return a Promise that is a Pair type of the underlying Promise and the passed Promise.
     * The returned Promise succeeds if both the Promises succeed. If either Promise fails
     * the returned Promise also fails.
     */
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
        addFailure(e -> promise.setException(e));
        promiseB.addFailure(e -> promise.setException(e));
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
        Block<T> success = v -> {
            if (done.compareAndSet(false, true)) {
                promise.set(v);
            }
        };
	Block<Throwable> fail = e -> {
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
    public Promise<T> rescue(Mapper<Throwable, T> mapper) {
        Promise<T> promise = new Promise<>();
        promise.link(this);
        addSuccess(v -> promise.set(v));
        addFailure(e -> promise.set(mapper.map(e)));
        return promise;
    }

    /**
     * Raise a signal to this Promise and all its linked Promises.
     */
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

    /**
     * If there is a signal raised on this Promise after the block is added, execute this Block.
     */
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
