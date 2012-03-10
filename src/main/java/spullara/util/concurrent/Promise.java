package spullara.util.concurrent;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.functions.*;

/**
 * A promise can be set from a separate thread and listeners and waiters on the
 * future will get the result.
 */
public class Promise<T> {

    public Promise() {}

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


    /**
     * New Future API section.
     */

    public <V> Promise<V> map(Mapper<T, V> mapper) {
	Promise<V> promise = new Promise<V>();
	addSuccess(value -> promise.set(mapper.map(value)));
	return promise;
    }

    public <V> Promise<V> flatMap(Mapper<T, Promise<V>> mapper) {
	Promise<V> promise = new Promise<V>();
	addSuccess(value1 -> mapper.map(value1).addSuccess(value2 -> promise.set(value2)));
	return promise;
    }

    public <B> Promise<Tuple2<T, B>> join(Promise<B> promiseB) {
	Promise<Tuple2<T,B>> promise = new Promise<>();
	AtomicReference ref = new AtomicReference();
	addSuccess(value -> {
		if (!ref.weakCompareAndSet(null, value)) {
		    promise.set(new Tuple2<>(value, (B) ref.get()));
		}
	    });
	promiseB.addSuccess(value -> {
		if (!ref.weakCompareAndSet(null, value)) {
		    promise.set(new Tuple2<>((T) ref.get(), value));
		}
	    });
	return promise;
    }
}