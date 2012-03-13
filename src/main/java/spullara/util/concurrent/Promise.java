package spullara.util.concurrent;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import spullara.util.functions.Block;
import spullara.util.functions.Blocks;
import spullara.util.functions.Mapper;

/**
 * You can use a Promise like an asychronous callback or you can block
 * on it like you would a Future.
 * <p/>
 * Loosely based on: http://twitter.github.com/scala_school/finagle.html
 */
public class Promise<T> {

    public Promise() {
    }

    public Promise(T t) {
        set(t);
    }

    public Promise(Throwable t) {
        setException(t);
    }

    // The setters use this to decide who wins
    private Semaphore set = new Semaphore(1);

    // The readers use this to wait for it to set
    private CountDownLatch read = new CountDownLatch(1);

    private T value;
    private Throwable throwable;

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

    private Block<Throwable> failed;
    private Block<T> success;

    public T get() throws InterruptedException, ExecutionException {
        read.await();
        if (throwable == null) {
            return value;
        } else {
            throw new ExecutionException(throwable);
        }
    }

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
     * Promise asynchronous API section.
     */

    public <V> Promise<V> map(final Mapper<T, V> mapper) {
        final Promise<V> promise = new Promise<>();
        addSuccess(new Block<T>() {
            public void apply(T value) {
                promise.set(mapper.map(value));
            }
        });
        return promise;
    }

    public <V> Promise<V> flatMap(final Mapper<T, Promise<V>> mapper) {
        final Promise<V> promise = new Promise<>();
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

    public <B> Promise<Tuple2<T, B>> join(Promise<B> promiseB) {
        final Promise<Tuple2<T, B>> promise = new Promise<>();
        final AtomicReference ref = new AtomicReference();
        addSuccess(new Block<T>() {
            @Override
            public void apply(T value) {
                if (!ref.weakCompareAndSet(null, value)) {
                    promise.set(new Tuple2<>(value, (B) ref.get()));
                }
            }
        });
        promiseB.addSuccess(new Block<B>() {
            @Override
            public void apply(B value) {
                if (!ref.weakCompareAndSet(null, value)) {
                    promise.set(new Tuple2<>((T) ref.get(), value));
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
        final Promise<T> promise = new Promise<>();
        final AtomicBoolean done = new AtomicBoolean();
        Block<T> block = new Block<T>() {
            @Override
            public void apply(T t) {
                if (done.compareAndSet(false, true)) {
                    promise.set(t);
                }
            }
        };
        addSuccess(block);
        promiseB.addSuccess(block);
        return promise;
    }

    public void foreach(Block<T> block) {
        addSuccess(block);
    }

    public Promise<T> onSuccess(Block<T> block) {
        addSuccess(block);
        return this;
    }

    public Promise<T> onFailure(Block<Throwable> block) {
        addFailure(block);
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
        final Promise<T> promise = new Promise<>();
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

}