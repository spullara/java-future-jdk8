package spullara.util;

import org.junit.Test;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.Assert.assertEquals;

public class LazyTest {

    public static final int NUM = 1000;

    @Test
    public void testLazy() {
        AtomicInteger ai = new AtomicInteger(0);
        Lazy<String> lazyString = new Lazy<String>(() -> "Value: " + ai.incrementAndGet());
        assertEquals(0, ai.get());
        assertEquals("Value: 1", lazyString.get());
        assertEquals(1, ai.get());
        assertEquals("Value: 1", lazyString.get());
        assertEquals(1, ai.get());
    }

    @Test
    public void testThreadedLazy() throws InterruptedException, ExecutionException {
        AtomicInteger executions = new AtomicInteger(0);
        AtomicInteger attempts = new AtomicInteger(0);
        Lazy<String> lazyString = new Lazy<String>(() -> "Value: " + executions.incrementAndGet());
        CyclicBarrier cyclicBarrier = new CyclicBarrier(NUM);
        ExecutorService es = Executors.newCachedThreadPool();
        for (int i = 0; i < NUM; i++) {
            es.submit(()->{
                cyclicBarrier.await();
                assertEquals("Value: 1", lazyString.get());
                assertEquals(1, executions.get());
                return attempts.incrementAndGet();
            });
        }
        es.shutdown();
        es.awaitTermination(1, TimeUnit.HOURS);
        assertEquals("Value: 1", lazyString.get());
        assertEquals(1, executions.get());
        assertEquals(NUM, attempts.get());
    }

}
