package spullara.util;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.Assert.assertEquals;
import static spullara.util.Option.option;

public class OptionTest {
    @Test
    public void testSimple() {
        Option<String> some = option("test");
        Option<String> none1 = Option.NONE;
        Option<String> none2 = option(null);

        AtomicInteger ai = new AtomicInteger(0);

        some.forEach( _ -> { ai.incrementAndGet(); });
        assertEquals(1, ai.get());
        none1.forEach(_-> { ai.incrementAndGet(); });
        none2.forEach(_-> { ai.incrementAndGet(); });
        assertEquals(1, ai.get());
        assertEquals(none1, none2);
    }
}

