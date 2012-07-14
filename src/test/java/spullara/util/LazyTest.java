package spullara.util;

import org.junit.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.Assert.assertEquals;

public class LazyTest {

    public static final int NUM = 1000;

    @Test
    public void testLazy() {
        final AtomicInteger ai = new AtomicInteger(0);
        Lazy<String> lazyString = new Lazy<String>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return "Value: " + ai.incrementAndGet();
            }
        });
        assertEquals(0, ai.get());
        assertEquals("Value: 1", lazyString.get());
        assertEquals(1, ai.get());
        assertEquals("Value: 1", lazyString.get());
        assertEquals(1, ai.get());
    }

    @Test
    public void testThreadedLazy() throws InterruptedException, ExecutionException {
        final AtomicInteger executions = new AtomicInteger(0);
        final AtomicInteger attempts = new AtomicInteger(0);
        final Lazy<String> lazyString = new Lazy<String>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return "Value: " + executions.incrementAndGet();
            }
        });
        final CyclicBarrier cyclicBarrier = new CyclicBarrier(NUM);
        ExecutorService es = Executors.newCachedThreadPool();
        for (int i = 0; i < NUM; i++) {
            es.submit(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    cyclicBarrier.await();
                    assertEquals("Value: 1", lazyString.get());
                    assertEquals(1, executions.get());
                    return attempts.incrementAndGet();
                }
            });
        }
        es.shutdown();
        es.awaitTermination(1, TimeUnit.HOURS);
        assertEquals("Value: 1", lazyString.get());
        assertEquals(1, executions.get());
        assertEquals(NUM, attempts.get());
    }

}
