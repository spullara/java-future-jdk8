package spullara.util.matching;

import org.junit.Test;

import java.util.Optional;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static junit.framework.Assert.assertEquals;
import static spullara.util.matching.Extractors.RegexExtractor;
import static spullara.util.matching.Match.match;
import static spullara.util.matching.Match.or;

public class MatchTest {

    @Test
    public void testSimple() {
        Match<String, Integer> matcher = match(
                (String s) -> s.equals("1") ? Optional.of(1) : null, s -> 1)
                .or(s -> s.equals("2") ? Optional.of(2) : Optional.<Integer>empty(), i -> 2)
                .or(s -> s.equals("3") ? Optional.of(3) : Optional.<Integer>empty(), i -> 3)
                .or(s -> {
                    if (s.equals("3")) return Optional.of(3);
                    else return null;
                }, i -> 3);
        or(matcher, RegexExtractor("([0-9]+)\\+([0-9]+)"), m -> parseInt(m.group(1)) + parseInt(m.group(2)));
        matcher.or(RegexExtractor("[0-9]+"), m -> parseInt(m.group(0)))
                .orElse(s -> 0);
        assertEquals(1, (int) matcher.check("1").get());
        assertEquals(2, (int) matcher.check("2").get());
        assertEquals(3, (int) matcher.check("3").get());
        assertEquals(4, (int) matcher.check("4").get());
        assertEquals(5, (int) matcher.check("2+3").get());
        assertEquals(0, (int) matcher.check("a").get());
    }

    @Test
    public void testCalculator() {
        String left = "(?<left>[-0-9]*(\\.[0-9]+)?)";
        String right = "(?<right>[-0-9]*(\\.[0-9]+)?)";
        String op = "(\\s*(?<op>[\\*\\+\\-\\/])\\s*)";
        // Handle simple binary operations
        Match<String, Double> calculator =
                match(RegexExtractor(left + op + right), m -> {
                    double l = parseDouble(m.group("left"));
                    double r = parseDouble(m.group("right"));
                    switch (m.group("op").charAt(0)) {
                        case '+':
                            return l + r;
                        case '-':
                            return l - r;
                        case '*':
                            return l * r;
                        case '/':
                            return l / r;
                        default:
                            throw new IllegalArgumentException();
                    }
                });
        // Handle a parenthesized expression
        calculator.or(RegexExtractor("\\((?<expr>.+)\\)(?<rest>" + op + "?.*)"), m -> calculator.check(calculator.check(m.group("expr")).get() + s(m.group("rest"))).get())
                .or(RegexExtractor(left), m -> parseDouble(m.group("left")));
        assertEquals(25.0, calculator.check("(2+3) * 5").get());
        assertEquals(5.0, calculator.check("2+3").get());
        assertEquals(5.0, calculator.check("(2+3)").get());
    }

    String s(String s) {
        return (s.trim() == null || s.equals("")) ? "" : " " + s.trim();
    }
}
