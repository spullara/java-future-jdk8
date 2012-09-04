package spullara.util.matching;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static spullara.util.Option.option;
import static spullara.util.matching.Extractors.RegexExtractor;
import static spullara.util.matching.Match.match;

public class MatchTest {

    @Test
    public void testSimple() {
        Match<String, Integer> matcher = match(
                (String s) -> s.equals("1") ? option(1) : null, (Integer s) -> 1)
                .or(s -> s.equals("2") ? option(2) : null, i -> 2)
                .or(s -> s.equals("3") ? option(3) : null, i -> 3)
                .or(RegexExtractor("[0-9]+"), m -> Integer.parseInt(m.group(0)))
                .orElse(s -> 0);
        assertEquals(1, (int) matcher.check("1").getOnly());
        assertEquals(2, (int) matcher.check("2").getOnly());
        assertEquals(3, (int) matcher.check("3").getOnly());
        assertEquals(4, (int) matcher.check("4").getOnly());
        assertEquals(0, (int) matcher.check("a").getOnly());
    }
}
