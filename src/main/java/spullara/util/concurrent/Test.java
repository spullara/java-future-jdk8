package spullara.util.concurrent;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.concurrent.*;

public class Test {
    public static void main(String[] args) throws Exception {
	PrintStream p = System.out;
	ExecutorService es = Executors.newCachedThreadPool();
	Promise<String> promise = Promises.execute(es, () -> { Thread.sleep(1000); return "Done."; });
	Promise<String> promise2 = Promises.execute(es, () -> { Thread.sleep(900); return "Done2."; });
	Promise<String> promise3 = new Promise<>("Constant");
	Promise<String> promise4 = Promises.execute(es, () -> { throw new RuntimeException("Promise4"); });

	promise.map(v -> { p.println("Set1: " + v); return "Really Done."; }).map(v -> p.println("Set2: " + v));

	promise.join(promise2).map( value -> p.println(value._1 + ", " + value._2) );

	promise2.flatMap(v -> promise3).map(v -> p.println("Flatmapped: " + v));
	promise2.flatMap(v -> promise4).onFailure(e -> p.println("Flat map failed: " + e));
	promise4.flatMap(v -> promise2).onFailure(e -> p.println("Flat map failed: " + e));

	promise.select(promise2).foreach(v -> p.println("Selected: " + v));
	promise2.select(promise).foreach(v -> p.println("Selected: " + v));

	p.println("Waited for this one: " + promise.get(1, TimeUnit.DAYS));

	try {
	    promise4.get();
	} catch (ExecutionException e) {
	    p.println("Get exception: " + e.getCause());
	}

	try {
	    promise4.join(promise).get();
	} catch (ExecutionException e) {
	    p.println("Join exception: " + e.getCause());
	}

	promise.onSuccess( v -> p.println("onSuccess: " + v)).onFailure( v -> p.println("onFailure: " + v)).ensure(() -> p.println("Ensured"));
	promise4.onSuccess( v -> p.println("onSuccess: " + v)).onFailure( v -> p.println("onFailure: " + v)).ensure(() -> p.println("Ensured"));

	promise4.rescue(e -> "Rescued!").map(v -> p.println(v));

	Promises.collect(Arrays.asList(promise, promise2, promise3)).map ( list -> {
		p.print("Collected: ");
		for (String s : list) {
		    p.print(s + " ");
		}
		p.println();
	    });

	promise.foreach(v -> p.println("Foreach: " + v));
	es.shutdown();
    }
}