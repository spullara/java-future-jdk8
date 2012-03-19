package spullara.util.concurrent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import spullara.util.functions.Block;

public class Promises {

    public static <T> Promise<T> execute(ExecutorService es, final Callable<T> callable) {
        final Promise<T> promise = new Promise<>();
        es.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    promise.set(callable.call());
                } catch (Throwable th) {
                    promise.setException(th);
                }
            }
        });
        return promise;
    }

    public static <T> Promise<List<T>> collect(List<Promise<T>> promises) {
        final Promise<List<T>> promiseOfList = new Promise<>();
        final int size = promises.size();
        final List<T> list = Collections.synchronizedList(new ArrayList<T>(size));
        if (promises.size() == 0) {
            promiseOfList.set(list);
        } else {
            for (Promise<T> promise : promises) {
                promise.onSuccess(new Block<T>() {
                    @Override
                    public void apply(T t) {
                        list.add(t);
                        if (list.size() == size) {
                            promiseOfList.set(list);
                        }
                    }
                }).onFailure(new Block<Throwable>() {
                    @Override
                    public void apply(Throwable throwable) {
                        promiseOfList.setException(throwable);
                    }
                });
            }
        }
        return promiseOfList;
    }
}