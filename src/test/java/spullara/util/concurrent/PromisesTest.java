package spullara.util.concurrent;

import org.junit.BeforeClass;
import org.junit.Test;
import spullara.util.functions.Block;
import spullara.util.functions.Mapper;
import spullara.util.functions.Pair;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
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
        final Promise<String> promise3 = new Promise<String>("Constant");
        final Promise<String> promise4 = Promises.execute(es, new Callable<String>() {
            @Override
            public String call() throws Exception {
                Thread.sleep(500);
                throw new RuntimeException("Promise4");
            }
        });
        Promise<String> promise5 = new Promise<String>(new RuntimeException("Promise5"));

        final Promise<String> result10 = new Promise<String>();
        try {
            promise4.onFailure(new Block<Throwable>() {
                @Override
                public void apply(Throwable throwable) {
                    result10.set("Failed");
                }
            }).get(0, TimeUnit.SECONDS);
            fail("Didn't timeout");
        } catch (TimeoutException te) {
        }

        try {
            promise5.map(new Mapper<String, Object>() {
                @Override
                public Object map(String s) {
                    return null;
                }
            }).get();
            fail("Didn't fail the map");
        } catch (ExecutionException e) {
        }
        final Promise<String> result3 = new Promise<String>();
        promise.select(promise2).foreach(new Block<String>() {
            @Override
            public void apply(String s) {
                result3.set("Selected: " + s);
            }
        });
        final Promise<String> result4 = new Promise<String>();
        promise2.select(promise).foreach(new Block<String>() {
            @Override
            public void apply(String s) {
                result4.set("Selected: " + s);
            }
        });
        assertEquals("Selected: Done2.", result3.get());
        assertEquals("Selected: Done2.", result4.get());

        Promise<Object> map1 = promise.join(promise2).map(new Mapper<Pair<String, String>, Object>() {
            @Override
            public Object map(Pair<String, String> stringStringTuple2) {
                return stringStringTuple2._1 + ", " + stringStringTuple2._2;
            }
        });
        Promise<Object> map2 = promise2.join(promise).map(new Mapper<Pair<String, String>, Object>() {
            @Override
            public Object map(Pair<String, String> stringStringTuple2) {
                return stringStringTuple2._1 + ", " + stringStringTuple2._2;
            }
        });
        assertEquals("Done., Done2.", map1.get());
        assertEquals("Done2., Done.", map2.get());

        final Promise<String> result1 = new Promise<String>();
        promise.select(promise4).foreach(new Block<String>() {
            @Override
            public void apply(String s) {
                result1.set("Selected: " + s);
            }
        });
        assertEquals("Selected: Done.", result1.get());
        assertEquals("Failed", result10.get());

        try {
            promise4.select(promise5).onFailure(new Block<Throwable>() {
                @Override
                public void apply(Throwable throwable) {
                    result1.set(throwable.getMessage());
                }
            }).get(1, TimeUnit.DAYS);
            fail("Should have failed");
        } catch (ExecutionException e) {
        }

        final CountDownLatch monitor = new CountDownLatch(2);
        Promise<String> onraised = Promises.execute(es, new Callable<String>() {
            @Override
            public String call() throws Exception {
                monitor.await();
                return "Interrupted";
            }
        });
        final Promise<Pair<String, String>> join = promise3.join(onraised.onRaise(new Block<Throwable>() {
            @Override
            public void apply(Throwable throwable) {
                monitor.countDown();
            }
        }));
        onraised.onRaise(new Block<Throwable>() {
            @Override
            public void apply(Throwable throwable) {
                monitor.countDown();
            }
        });

        Promise<String> map = promise.map(new Mapper<String, String>() {
            @Override
            public String map(String s) {
                return "Set1: " + s;
            }
        }).map(new Mapper<String, String>() {
            @Override
            public String map(String s) {
                join.raise(new CancellationException());
                return "Set2: " + s;
            }
        });

        assertEquals("Set2: Set1: Done.", map.get());
        assertEquals(new Pair<String, String>("Constant", "Interrupted"), join.get());

        try {
            promise.join(promise4).map(new Mapper<Pair<String, String>, Object>() {
                @Override
                public Object map(Pair<String, String> stringStringTuple2) {
                    return stringStringTuple2._1 + ", " + stringStringTuple2._2;
                }
            }).get();
            fail("Should have failed");
        } catch (ExecutionException ee) {
        }

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
        }).get());

        final Promise<String> result11 = new Promise<>();
        try {
            promise2.flatMap(new Mapper<String, Promise<String>>() {
                @Override
                public Promise<String> map(String s) {
                    return promise4;
                }
            }).onFailure(new Block<Throwable>() {
                @Override
                public void apply(Throwable throwable) {
                  result11.set("Failed");
                }
            }).get();
            fail("Should have failed");
        } catch (ExecutionException ee) {
            assertEquals("Failed", result11.get());
        }

        final Promise<String> result2 = new Promise<String>();
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

        final Promise<String> result5 = new Promise<String>();
        final Promise<String> result6 = new Promise<String>();
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

        final Promise<String> result7 = new Promise<String>();
        final Promise<String> result8 = new Promise<String>();
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
        assertEquals("Was Constant", promise3.rescue(new Mapper<Throwable, String>() {
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

        assertEquals(Arrays.asList(),
                Promises.collect(Collections.<Promise<String>>emptyList()).map(new Mapper<List<String>, List<String>>() {
                    @Override
                    public List<String> map(List<String> list) {
                        return list;
                    }
                }).get());

        try {
            Promises.collect(Arrays.asList(promise, promise2, promise4)).map(new Mapper<List<String>, List<String>>() {
                @Override
                public List<String> map(List<String> list) {
                    return list;
                }
            }).get();
            fail("Should have failed");
        } catch (ExecutionException ee) {
        }

        final Promise<String> result9 = new Promise<String>();
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