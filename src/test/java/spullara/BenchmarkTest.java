package spullara;

import org.junit.Test;
import spullara.util.Benchmarker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class BenchmarkTest {
    @Test
    public void testVsFor() throws Exception {
        Benchmarker bm = new Benchmarker(10000000);
        List<String> list = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            list.add("foorbar" + i);
        }
        AtomicInteger ai = new AtomicInteger();
        bm.execute("for loop", () -> {
            int i = 0;
            for (String s : list) {
                if (s.endsWith("1")) {
                    i++;
                }
            }
            ai.addAndGet(i);
        });
        System.out.println(ai);
        bm.execute("sequential stream", () -> {
            ai.addAndGet((int) list.stream().filter(v -> v.endsWith("1")).count());
        });
        bm.execute("parallel stream", () -> {
            ai.addAndGet((int) list.stream().parallel().filter(v -> v.endsWith("1")).count());
        });
        System.out.println(ai);
        bm.report();
    }
}
