package spullara.util.concurrent;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import spullara.util.functions.Block;
import spullara.util.functions.Mapper;

public class Test {
    public static void main(String[] args) throws Exception {
        final PrintStream p = System.out;
        ExecutorService es = Executors.newCachedThreadPool();
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
        final Object object = new Object();
        promise3.join(Promises.execute(es, new Callable<String>() {
            @Override
            public String call() throws Exception {
                synchronized (object) {
                    object.wait();
                }
                p.println("Interrupted");
                return null;
            }
        }).onRaise(new Block<Throwable>() {
            @Override
            public void apply(Throwable throwable) {
                synchronized (object) {
                    object.notifyAll();
                }
            }
        }));

        promise.map(new Mapper<String, String>() {
            @Override
            public String map(String s) {
                p.println("Set1: " + s);
                return "Really Done.";
            }
        }).map(new Mapper<String, String>() {
            @Override
            public String map(String s) {
                p.println("Set2: " + s);
                promise3.raise(new CancellationException());
                return null;
            }
        });

        promise.join(promise2).map(new Mapper<Tuple2<String, String>, Object>() {
            @Override
            public Object map(Tuple2<String, String> stringStringTuple2) {
                p.println(stringStringTuple2._1 + ", " + stringStringTuple2._2);
                return null;
            }
        });

        promise2.flatMap(new Mapper<String, Promise<String>>() {
            @Override
            public Promise<String> map(String s) {
                return promise3;
            }
        }).map(new Mapper<String, Object>() {
            @Override
            public Object map(String s) {
                p.println("Flatmapped: " + s);
                return null;
            }
        });

        promise2.flatMap(new Mapper<String, Promise<String>>() {
            @Override
            public Promise<String> map(String s) {
                return promise3;
            }
        }).onFailure(new Block<Throwable>() {
            @Override
            public void apply(Throwable throwable) {
                p.println("Flat map failed: " + throwable);
            }
        });

        promise4.flatMap(new Mapper<String, Promise<String>>() {
            @Override
            public Promise<String> map(String s) {
                return promise2;
            }
        }).onFailure(new Block<Throwable>() {
            @Override
            public void apply(Throwable throwable) {
                p.println("Flat map failed: " + throwable);
            }
        });

        promise.select(promise2).foreach(new Block<String>() {
            @Override
            public void apply(String s) {
                p.println("Selected: " + s);
            }
        });
        promise2.select(promise).foreach(new Block<String>() {
            @Override
            public void apply(String s) {
                p.println("Selected: " + s);
            }
        });

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

        promise.onSuccess(new Block<String>() {
            @Override
            public void apply(String s) {
                p.println("onSuccess: " + s);
            }
        }).onFailure(new Block<Throwable>() {
            @Override
            public void apply(Throwable throwable) {
                p.println("onFailure: " + throwable);
            }
        }).ensure(new Runnable() {
            @Override
            public void run() {
                p.println("Ensured");
            }
        });
        promise4.onSuccess(new Block<String>() {
            @Override
            public void apply(String s) {
                p.println("onSuccess: " + s);
            }
        }).onFailure(new Block<Throwable>() {
            @Override
            public void apply(Throwable throwable) {
                p.println("onFailure: " + throwable);
            }
        }).ensure(new Runnable() {
            @Override
            public void run() {
                p.println("Ensured");
            }
        });

        promise4.rescue(new Mapper<Throwable, String>() {
            @Override
            public String map(Throwable throwable) {
                return "Rescued!";
            }
        }).map(new Mapper<String, Object>() {
            @Override
            public Object map(String s) {
                p.println(s);
                return null;
            }
        });

        Promises.collect(Arrays.asList(promise, promise2, promise3)).map(new Mapper<List<String>, Object>() {
            @Override
            public Object map(List<String> list) {
                p.print("Collected: ");
                for (String s : list) {
                    p.print(s + " ");
                }
                p.println();
                return null;
            }
        });

        promise.foreach(new Block<String>() {
            @Override
            public void apply(String s) {
                p.println("Foreach: " + s);
            }
        });
        es.shutdown();
        es.awaitTermination(1, TimeUnit.DAYS);
    }
}