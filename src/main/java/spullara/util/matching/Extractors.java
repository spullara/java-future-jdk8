package spullara.util.matching;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Common extractors
 */
public class Extractors {
    public static Extractor<String, Matcher> RegexExtractor(String pattern) {
        Pattern p = Pattern.compile(pattern);
        return s -> {
            Matcher matcher = p.matcher(s);
            if (matcher.matches()) return Optional.of(matcher); else return Optional.empty();
        };
    }

    public static Extractor<String, Integer> IntegerExtractor() {
        return s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException nfe) {
                return Optional.empty();
            }
        };
    }

    public static Extractor<String, Long> LongExtractor() {
        return s -> {
            try {
                return Optional.of(Long.parseLong(s));
            } catch (NumberFormatException nfe) {
                return Optional.empty();
            }
        };
    }

    public static Extractor<String, Double> DoubleExtractor() {
        return s -> {
            try {
                return Optional.of(Double.parseDouble(s));
            } catch (NumberFormatException nfe) {
                return Optional.empty();
            }
        };
    }

}
