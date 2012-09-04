package spullara.util.matching;

import spullara.util.Option;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static spullara.util.Option.option;

/**
 * Common extractors
 */
public class Extractors {
    public static Extractor<String, Matcher> RegexExtractor(String pattern) {
        Pattern p = Pattern.compile(pattern);
        return s -> {
            Matcher matcher = p.matcher(s);
            return matcher.matches() ? option(matcher) : Option.<Matcher>none();
        };
    }

    public static Extractor<String, Integer> IntegerExtractor() {
        return s -> {
            try {
                return option(Integer.parseInt(s));
            } catch (NumberFormatException nfe) {
                return Option.<Integer>none();
            }
        };
    }

    public static Extractor<String, Long> LongExtractor() {
        return s -> {
            try {
                return option(Long.parseLong(s));
            } catch (NumberFormatException nfe) {
                return Option.<Long>none();
            }
        };
    }

    public static Extractor<String, Double> DoubleExtractor() {
        return s -> {
            try {
                return option(Double.parseDouble(s));
            } catch (NumberFormatException nfe) {
                return Option.<Double>none();
            }
        };
    }

}
