package spullara.util.concurrent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Test {
    public static void main(String[] args) throws Exception {
	ExecutorService es = Executors.newCachedThreadPool();
	Promise<String> promise = new Promise<>();
	Promise<String> promise2 = new Promise<>();
	es.submit(() -> { Thread.sleep(900); promise.set("Done."); });
	es.submit(() -> { Thread.sleep(1000); promise2.set("Done2."); });

	promise.map(v -> { System.out.println("Set1: " + v); return "Really Done."; }).map(v -> System.out.println("Set2: " + v));
	promise.join(promise2).map( value -> System.out.println(value._1 + ", " + value._2) );
	System.out.println(promise.get(1, TimeUnit.DAYS));
	es.shutdown();
    }
}