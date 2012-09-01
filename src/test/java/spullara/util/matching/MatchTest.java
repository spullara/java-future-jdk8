package spullara.util.matching;

import org.junit.Test;
import spullara.util.Option;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static junit.framework.Assert.assertEquals;
import static spullara.util.Option.option;
import static spullara.util.matching.Match.match;

public class MatchTest {

    static class RegexExtractor implements Extractor<String, Matcher> {
        private final Pattern pattern;

        RegexExtractor(String pattern) {
            this.pattern = Pattern.compile(pattern);
        }
        @Override
        public Option<Matcher> unapply(String s) {
            Matcher matcher = pattern.matcher(s);
            return matcher.matches() ? option(matcher) : Option.<Matcher>none();
        }
    }

    @Test
    public void testSimple() {
        RegexExtractor numbers = new RegexExtractor("[0-9]+");
        Match<String, Integer> matcher = match(
                (String s) -> s.equals("1") ? option(1) : null, (Integer s) -> 1)
                .or(s -> s.equals("2") ? option(2) : null, (Integer i) -> 2)
                .or(s -> s.equals("3") ? option(3) : null, (Integer i) -> 3)
                .or(numbers, m -> Integer.parseInt(m.group(0)))
                .orElse(s -> 0);
        assertEquals(1, (int) matcher.check("1").getOnly());
        assertEquals(2, (int) matcher.check("2").getOnly());
        assertEquals(3, (int) matcher.check("3").getOnly());
        assertEquals(4, (int) matcher.check("4").getOnly());
        assertEquals(0, (int) matcher.check("a").getOnly());
    }
}
