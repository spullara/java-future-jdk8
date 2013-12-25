package spullara.util.matching;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class Match<T, V> {

    private List<Extractor> extractors = new ArrayList<>();
    private List<Function> matched = new ArrayList<>();
    private Function<T, V> other;

    public static <T, V, W> Match<T, V> match(Extractor<T, W> e, Function<W, V> c) {
        return new Match<T, V>().or(e, c);
    }

    public <W> Match<T, V> or(Extractor<T, W> e, Function<W, V> c) {
        extractors.add(e);
        matched.add(c);
        return this;
    }

    public static <T, V, W> void or(Match<T, V> m, Extractor<T, W> e, Function<W, V> c) {
        m.extractors.add(e);
        m.matched.add(c);
    }

    public Match<T, V> orElse(Function<T, V> other) {
        this.other = other;
        return this;
    }

    public Optional<V> check(T value) {
        for (int i = 0; i < extractors.size(); i++) {
            Optional isMatched = extractors.get(i).unapply(value);
            if (isMatched != null && isMatched.isPresent()) {
                return (Optional<V>) Optional.of(matched.get(i).apply(isMatched.get()));
            }
        }
        if (other == null) {
            return Optional.empty();
        }
        return Optional.of(other.apply(value));
    }
}
