package spullara.util.concurrent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.functions.*;

/**
 * A promise can be set from a separate thread and listeners and waiters on the
 * future will get the result.
 */
public class Promise<T> {
    
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

    public <V> Promise<V> map(Mapper<T, V> mapper) {
	Promise<V> promise = new Promise<V>();
	Block<T> mapBlock = value -> promise.set(mapper.map(value));
	synchronized (this) {
	    if (success == null) {
		success = mapBlock;
	    } else {
		success = Blocks.chain(success, mapBlock);
	    } 
	}
	return promise;
    }
}