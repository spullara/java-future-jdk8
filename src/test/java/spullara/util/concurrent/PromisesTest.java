package spullara.util.concurrent;

import org.junit.BeforeClass;
import org.junit.Test;
import spullara.util.functions.Block;
import spullara.util.functions.Mapper;
import spullara.util.functions.Pair;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;

public class PromisesTest {

    private static ExecutorService es;

    @BeforeClass
    public static void setup() {
        es = Executors.newCachedThreadPool();
    }

    @Test
    public void testPromises() throws Exception {
        final PrintStream p = System.out;
        Promise<String> promise = Promises.execute(es, new Callable<String>() {
            @Override
            public String call() throws Exception {
                Thread.sleep(1000);
                return "Done.";
            }
        });
        final Promise<String> promise2 = Promises.execute(es, new Callable<String>() {
            @Override
            public String call() throws Exception {
                Thread.sleep(900);
                return "Done2.";
            }
        });
        final Promise<String> promise3 = new Promise<>("Constant");
        Promise<String> promise4 = Promises.execute(es, new Callable<String>() {
            @Override
            public String call() throws Exception {
                throw new RuntimeException("Promise4");
            }
        });

        final Promise<String> result3 = new Promise<>();
        promise.select(promise2).foreach(new Block<String>() {
            @Override
            public void apply(String s) {
                result3.set("Selected: " + s);
            }
        });
        assertEquals("Selected: Done2.", result3.get());
        final Promise<String> result4 = new Promise<>();
        promise2.select(promise).foreach(new Block<String>() {
            @Override
            public void apply(String s) {
                result4.set("Selected: " + s);
            }
        });
        assertEquals("Selected: Done2.", result4.get());

        final Object monitor = new Object();
        Promise<Pair<String, String>> join = promise3.join(Promises.execute(es, new Callable<String>() {
            @Override
            public String call() throws Exception {
                synchronized (monitor) {
                    monitor.wait();
                }
                return "Interrupted";
            }
        }).onRaise(new Block<Throwable>() {
            @Override
            public void apply(Throwable throwable) {
                synchronized (monitor) {
                    monitor.notifyAll();
                }
            }
        }));

        Promise<String> map = promise.map(new Mapper<String, String>() {
            @Override
            public String map(String s) {
                return "Set1: " + s;
            }
        }).map(new Mapper<String, String>() {
            @Override
            public String map(String s) {
                promise3.raise(new CancellationException());
                return "Set2: " + s;
            }
        });

        assertEquals("Set2: Set1: Done.", map.get());
        assertEquals(new Pair<>("Constant", "Interrupted"), join.get());

        assertEquals("Done., Done2.", promise.join(promise2).map(new Mapper<Pair<String, String>, Object>() {
            @Override
            public Object map(Pair<String, String> stringStringTuple2) {
                return stringStringTuple2._1 + ", " + stringStringTuple2._2;
            }
        }).get());

        assertEquals("Flatmapped: Constant", promise2.flatMap(new Mapper<String, Promise<String>>() {
            @Override
            public Promise<String> map(String s) {
                return promise3;
            }
        }).map(new Mapper<String, Object>() {
            @Override
            public Object map(String s) {
                return "Flatmapped: " + s;
            }
        }).get());

        assertEquals("Constant", promise2.flatMap(new Mapper<String, Promise<String>>() {
            @Override
            public Promise<String> map(String s) {
                return promise3;
            }
        }).onFailure(new Block<Throwable>() {
            @Override
            public void apply(Throwable throwable) {
                // failed
            }
        }).get());

        final Promise<String> result2 = new Promise<>();
        promise4.flatMap(new Mapper<String, Promise<String>>() {
            @Override
            public Promise<String> map(String s) {
                return promise2;
            }
        }).onFailure(new Block<Throwable>() {
            @Override
            public void apply(Throwable throwable) {
                result2.set("Flat map failed: " + throwable);
            }
        });
        assertEquals("Flat map failed: java.lang.RuntimeException: Promise4", result2.get());

        assertEquals("Done.", promise.get(1, TimeUnit.DAYS));

        try {
            promise4.get();
            fail("Should have thrown");
        } catch (ExecutionException e) {
        }

        try {
            promise4.join(promise).get();
            fail("Should have thrown");
        } catch (ExecutionException e) {
        }

        final Promise<String> result5 = new Promise<>();
        final Promise<String> result6 = new Promise<>();
        promise.onSuccess(new Block<String>() {
            @Override
            public void apply(String s) {
                result5.set("onSuccess: " + s);
            }
        }).onFailure(new Block<Throwable>() {
            @Override
            public void apply(Throwable throwable) {
                result5.set("onFailure: " + throwable);
            }
        }).ensure(new Runnable() {
            @Override
            public void run() {
                result6.set("Ensured");
            }
        });
        assertEquals("onSuccess: Done.", result5.get());
        assertEquals("Ensured", result6.get());

        final Promise<String> result7 = new Promise<>();
        final Promise<String> result8 = new Promise<>();
        promise4.onSuccess(new Block<String>() {
            @Override
            public void apply(String s) {
                result7.set("onSuccess: " + s);
            }
        }).onFailure(new Block<Throwable>() {
            @Override
            public void apply(Throwable throwable) {
                result7.set("onFailure: " + throwable);
            }
        }).ensure(new Runnable() {
            @Override
            public void run() {
                result8.set("Ensured");
            }
        });
        assertEquals("onFailure: java.lang.RuntimeException: Promise4", result7.get());
        assertEquals("Ensured", result8.get());

        assertEquals("Was Rescued!", promise4.rescue(new Mapper<Throwable, String>() {
            @Override
            public String map(Throwable throwable) {
                return "Rescued!";
            }
        }).map(new Mapper<String, Object>() {
            @Override
            public Object map(String s) {
                return "Was " + s;
            }
        }).get());

        assertEquals(Arrays.asList("Done.", "Done2.", "Constant"),
                Promises.collect(Arrays.asList(promise, promise2, promise3)).map(new Mapper<List<String>, List<String>>() {
                    @Override
                    public List<String> map(List<String> list) {
                        return list;
                    }
                }).get());

        final Promise<String> result9 = new Promise<>();
        promise.foreach(new Block<String>() {
            @Override
            public void apply(String s) {
                result9.set("Foreach: " + s);
            }
        });
        assertEquals("Foreach: Done.", result9.get());

        es.shutdown();
        es.awaitTermination(1, TimeUnit.DAYS);
    }
}