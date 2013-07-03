package spullara.util;

import org.junit.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.Assert.assertEquals;
import static spullara.util.Lazy.lazy;

public class LazyTest {

    public static final int NUM = 1000;

    @Test
    public void testLazy() {
        AtomicInteger ai = new AtomicInteger(0);
        Lazy<String> lazyString = lazy(() -> "Value: " + ai.incrementAndGet());
        assertEquals(0, ai.get());
        assertEquals("Value: 1", lazyString.get());
        assertEquals(2, ai.incrementAndGet());
        assertEquals("Value: 1", lazyString.get());
    }

    @Test
    public void testThreadedLazy() throws InterruptedException, ExecutionException {
        AtomicInteger executions = new AtomicInteger(0);
        AtomicInteger attempts = new AtomicInteger(0);
        Lazy<String> lazyString = lazy(() -> "Value: " + executions.incrementAndGet());
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
