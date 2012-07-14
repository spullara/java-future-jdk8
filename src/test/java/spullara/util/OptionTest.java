package spullara.util;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.Assert.assertEquals;
import static spullara.util.Option.none;
import static spullara.util.Option.option;

public class OptionTest {
    @Test
    public void testSimple() {
        Option<String> some = option("test");
        Option<String> none1 = none();
        Option<String> none2 = option(null);

        AtomicInteger ai = new AtomicInteger(0);

        for (String s : some) {
            ai.incrementAndGet();
        }
        assertEquals(1, ai.get());
        for (String s : none1) {
            ai.incrementAndGet();
        }
        assertEquals(1, ai.get());
        for (String s : none2) {
            ai.incrementAndGet();
        }
        assertEquals(1, ai.get());
        assertEquals(none1, none2);
    }
}

