package spullara.util.concurrent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

public class Promises {

    public static <T> Promise<T> execute(ExecutorService es, Callable<T> callable) {
	Promise<T> promise = new Promise<>();
	es.submit(() -> {
		try {
		    promise.set(callable.call());
		} catch (Throwable th) {
		    promise.setException(th);
		}
	    });
	return promise;
    }

    public static <T> Promise<? extends List<T>> collect(List<Promise<T>> promises) {
	Promise<List<T>> promiseOfList = new Promise<>();
	int size = promises.size();
	List<T> list = Collections.synchronizedList(new ArrayList<T>(size));
	if (promises.size() == 0) {
	    promiseOfList.set(list);
	} else {
	    for (Promise<T> promise : promises) {
		promise.onSuccess(v -> {
			list.add(v);
			if (list.size() == size) {
			    promiseOfList.set(list);
			}
		    }).onFailure(e -> { promiseOfList.setException(e); });
	    }
	}
	return promiseOfList;
    }
}