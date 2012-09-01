package spullara.util;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.functions.Mapper;

import static junit.framework.Assert.assertEquals;
import static spullara.util.Option.none;
import static spullara.util.Option.option;

public class OptionTest {
    @Test
    public void testSimple() {
        Option<String> some = option("test");
        Option<String> none1 = none();
        Option<String> none2 = option(null);

        Mapper<String, Option<String>> test = s -> {
            if (s.equals("")) return Option.<String>none(); else return option(s);
        };

        AtomicInteger ai = new AtomicInteger(0);

        some.forEach( _ -> { ai.incrementAndGet(); });
        assertEquals(1, ai.get());
        none1.forEach(_-> { ai.incrementAndGet(); });
        assertEquals(1, ai.get());
        none2.forEach(_-> { ai.incrementAndGet(); });
        assertEquals(1, ai.get());
        assertEquals(none1, none2);
    }
}

