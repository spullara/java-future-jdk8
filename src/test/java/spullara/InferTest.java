package spullara;

import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InferTest {
    void call() {
        Thread.dumpStack();
    }
//    void call(int i) {}

    @Test
    public void test() {
        ExecutorService es = Executors.newCachedThreadPool();
        es.submit(this::call);
    }
}