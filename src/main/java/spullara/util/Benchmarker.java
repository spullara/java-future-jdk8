package spullara.util;

import java.util.*;
import java.util.concurrent.Callable;

public class Benchmarker {

    private final int times;

    public Benchmarker(int times) {
        this.times = times;
    }

    private Map<String, Double> results = new LinkedHashMap<>();

    public interface ExceptionRunnable {
        void run() throws Exception;
    }

    public void execute(String message, ExceptionRunnable runnable) throws Exception {
        long start = System.nanoTime();
        for (int i = 0; i < times; i++) {
            runnable.run();
        }
        long end = System.nanoTime();
        results.put(message, ((double) (end - start)) / times);
    }

    public void report() {
        double max = results.values().stream().max(Double::compare).orElse(1.0);
        results.entrySet().stream().sorted((e1, e2) -> e1.getValue() - e2.getValue() < 0 ? -1 : 1).forEach(e -> {
            System.out.println(e.getKey() + ": " + e.getValue() + " (" + e.getValue() / max + ")");
        });
    }
}
