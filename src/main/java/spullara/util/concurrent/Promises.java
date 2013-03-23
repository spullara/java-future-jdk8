package spullara.util.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Promises {

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

    public static <T> Promise<? extends List<T>> collect(List<Promise<T>> promises) {
        Promise<List<T>> promiseOfList = new Promise<>();
        int size = promises.size();
        List<T> list = Collections.synchronizedList(new ArrayList<>(size));
        if (promises.size() == 0) {
            promiseOfList.set(list);
        } else {
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
}