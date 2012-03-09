package spullara.util.concurrent;

import java.util.concurrent.TimeUnit;

public class Test {
    public static void main(String[] args) throws Exception {
	Promise<String> promise = new Promise<>();
	Thread thread = new Thread(() -> {
		    try {
			Thread.sleep(1000);
			promise.set("Done.");
		    } catch(InterruptedException ie) {}
	    });
	thread.setDaemon(true);
	thread.start();
	promise.map(v -> System.out.println("Set:" + v));
	System.out.println(promise.get(1, TimeUnit.DAYS));
    }
}