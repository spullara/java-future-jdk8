package spullara.util.concurrent;

import java.util.concurrent.*;

public class Test {
    public static void main(String[] args) throws Exception {
	Promise<String> promise = new Promise<>();
	Promise<String> promise2 = new Promise<>();
	Promise<String> promise3 = new Promise<>("Constant");
	Promise<String> promise4 = new Promise<>(new RuntimeException());

	ExecutorService es = Executors.newCachedThreadPool();
	es.submit(() -> { Thread.sleep(1000); promise.set("Done."); });
	es.submit(() -> { Thread.sleep(900); promise2.set("Done2."); });

	promise.map(v -> { System.out.println("Set1: " + v); return "Really Done."; }).map(v -> System.out.println("Set2: " + v));
	promise.join(promise2).map( value -> System.out.println(value._1 + ", " + value._2) );
	promise2.flatMap(v -> promise3).map(v -> System.out.println("Flatmapped: " + v));
	promise.select(promise2).foreach(v -> System.out.println("Selected: " + v));
	promise2.select(promise).foreach(v -> System.out.println("Selected: " + v));

	System.out.println("Waited for this one: " + promise.get(1, TimeUnit.DAYS));

	try {
	    promise4.get();
	} catch (ExecutionException e) {
	    System.out.print("Get exception: ");
	    e.getCause().printStackTrace();
	}

	try {
	    promise4.join(promise).get();
	} catch (ExecutionException e) {
	    System.out.print("Join exception: ");
	    e.getCause().printStackTrace();
	}

	promise.onSuccess( v -> System.out.println("onSuccess: " + v)).onFailure( v -> System.out.println("onFailure: " + v)).ensure(() -> System.out.println("Ensured"));
	promise4.onSuccess( v -> System.out.println("onSuccess: " + v)).onFailure( v -> System.out.println("onFailure: " + v)).ensure(() -> System.out.println("Ensured"));

	promise4.rescue(e -> "Rescued!").map(v -> System.out.println(v));

	promise.foreach(v -> System.out.println("Foreach: " + v));
	es.shutdown();
    }
}