package spullara.util.concurrent;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.functions.*;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PromisesTest {

    public static void main(String[] args) throws Exception {
        PromisesTest pt = new PromisesTest();
        pt.setup();
        pt.testPromises();
        System.out.println("Success.");
    }

    private static ExecutorService es;

    @BeforeClass
    public static void setup() {
        es = Executors.newCachedThreadPool();
    }

    @Test
    public void testPromises() throws Exception {
        Promise<String> promise = Promises.execute(es, () -> {Thread.sleep(1000); return "Done.";});
        Promise<String> promise2 = Promises.execute(es, () -> {Thread.sleep(900); return "Done2.";});
        Promise<String> promise3 = new Promise<>("Constant");
        Promise<String> promise4 = Promises.execute(es, () -> {Thread.sleep(500);
        throw new RuntimeException("Promise4");});
        Promise<String> promise5 = new Promise<>(new RuntimeException("Promise5"));
        Promise<String> promise6 = Promises.execute(es, () -> {Thread.sleep(1000); return "Done.";});
        promise6.cancel(true);

        try {
            assertTrue(promise6.isCancelled());
            assertTrue(promise6.isDone());
            promise6.get();
            fail("Was not cancelled");
        } catch (CancellationException ce) {
        }

        Promise<String> result10 = new Promise<>();
        try {
            promise4.onFailure(e -> { result10.set("Failed"); }).get(0, TimeUnit.SECONDS);
            fail("Didn't timeout");
        } catch (TimeoutException te) {
        }

        try {
            promise5.map(v -> null).get();
            fail("Didn't fail");
        } catch (ExecutionException ee) {
        }

        Promise<String> result3 = new Promise<>();
        promise.select(promise2).onSuccess(v -> { result3.set("Selected: " + v); });
        final Promise<String> result4 = new Promise<>();
        promise2.select(promise).onSuccess(v -> { result4.set("Selected: " + v); });
        assertEquals("Selected: Done2.", result3.get());
        assertEquals("Selected: Done2.", result4.get());

        Promise<String> map1 = promise.join(promise2).map( (Pair<String, String> value) -> value._1 + ", " + value._2);
        Promise<String> map2 = promise2.join(promise).map( (Pair<String, String> value) -> value._1 + ", " + value._2);
        assertEquals("Done., Done2.", map1.get());
        assertEquals("Done2., Done.", map2.get());

        final Promise<String> result1 = new Promise<>();
        promise.select(promise4).onSuccess(s -> { result1.set("Selected: " + s); });
        assertEquals("Selected: Done.", result1.get());
        assertEquals("Failed", result10.get());

        try {
            promise4.select(promise5).onFailure( (Throwable e) -> { result1.set(e.getMessage()); }).get();
            fail("Didn't fail");
        } catch (ExecutionException ee) {
        }

        final CountDownLatch monitor = new CountDownLatch(2);
        Promise<String> onraise = Promises.execute(es, (Callable<String>) () -> {
                monitor.await();
                return "Interrupted";
        });
        Promise<Pair<String, String>> join = promise3.join(onraise);
        onraise.onRaise(e -> {
                monitor.countDown();
        });
        onraise.onRaise(e -> {
                monitor.countDown();
        });

        Promise<String> map = promise.map( (String v) -> "Set1: " + v).map( (String v) -> {
                join.raise(new CancellationException());
                return "Set2: " + v;
        });

        assertEquals("Set2: Set1: Done.", map.get());
        assertEquals(new Pair<>("Constant", "Interrupted"), join.get());

        try {
            promise.join(promise4).map( (Pair<String, String> value) -> value._1 + ", " + value._2).get();
            fail("Didn't fail");
        } catch (ExecutionException ee) {
        }

        assertEquals("Flatmapped: Constant", promise2.flatMap(v -> promise3).map(v -> "Flatmapped: " + v).get());

        Promise<String> result11 = new Promise<>();
        try {
            promise2.flatMap(v -> promise4).onFailure(e -> { result11.set("Failed"); }).get();
        } catch (ExecutionException ee) {
            assertEquals("Failed", result11.get());
        }

        Promise<String> result2 = new Promise<>();
        promise4.flatMap(v -> promise2).onFailure(e -> { result2.set("Flat map failed: " + e); });
        assertEquals("Flat map failed: java.lang.RuntimeException: Promise4", result2.get());

        assertEquals("Done.", promise.get(1, TimeUnit.DAYS));

        try {
            promise4.get();
            fail("Didn't fail");
        } catch (ExecutionException e) {
        }

        try {
            promise4.join(promise).get();
            fail("Didn't fail");
        } catch (ExecutionException e) {
        }

        Promise<String> result5 = new Promise<>();
        Promise<String> result6 = new Promise<>();
        promise.onSuccess(s -> { result5.set("onSuccess: " + s); }).onFailure(e -> { result5.set("onFailure: " + e); }).ensure(() -> { result6.set("Ensured"); });
        assertEquals("onSuccess: Done.", result5.get());
        assertEquals("Ensured", result6.get());

        Promise<String> result7 = new Promise<>();
        Promise<String> result8 = new Promise<>();
        promise4.onSuccess(s -> { result7.set("onSuccess: " + s); }).onFailure(e -> { result7.set("onFailure: " + e); }).ensure(() -> { result8.set("Ensured"); });
        assertEquals("onFailure: java.lang.RuntimeException: Promise4", result7.get());
        assertEquals("Ensured", result8.get());

        assertEquals("Was Rescued!", promise4.rescue(e -> "Rescued!").map(v -> "Was " + v).get());
        assertEquals("Was Constant", promise3.rescue(e -> "Rescued!").map(v -> "Was " + v).get());

        assertEquals(Arrays.asList("Done.", "Done2.", "Constant"), Promises.collect(Arrays.asList(promise, promise2, promise3)).get());
        assertEquals(Arrays.asList(), Promises.collect(new ArrayList<Promise<String>>()).get());
        try {
            assertEquals(Arrays.asList("Done.", "Done2.", "Constant"), Promises.collect(Arrays.asList(promise, promise4, promise3)).get());
            fail("Didn't fail");
        } catch (ExecutionException ee) {
        }

        Promise<String> result9 = new Promise<>();
        promise.onSuccess(v -> { result9.set("onSuccess: " + v); });
        assertEquals("onSuccess: Done.", result9.get());

        es.shutdown();
    }
}