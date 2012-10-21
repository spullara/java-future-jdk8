package spullara.util.matching;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.functions.Mapper;

public class Match<T, V> {

    private List<Extractor> extractors = new ArrayList<>();
    private List<Mapper> matched = new ArrayList<>();
    private Mapper<T, V> other;

    public static <T, V, W> Match<T, V> match(Extractor<T, W> e, Mapper<W, V> c) {
        Match match = new Match();
        match.or(e, c);
        return match;
    }

    public <W> Match<T, V> or(Extractor<T, W> e, Mapper<W, V> c) {
        extractors.add(e);
        matched.add(c);
        return this;
    }

    public static <T, V, W> void or(Match<T, V> m, Extractor<T, W> e, Mapper<W, V> c) {
        m.extractors.add(e);
        m.matched.add(c);
    }

    public Match<T, V> orElse(Mapper<T, V> other) {
        this.other = other;
        return this;
    }

    public Optional<V> check(T value) {
        for (int i = 0; i < extractors.size(); i++) {
            Optional isMatched = extractors.get(i).unapply(value);
            if (isMatched != null && isMatched.isPresent()) {
                return (Optional<V>) new Optional(matched.get(i).map(isMatched.get()));
            }
        }
        if (other == null) {
            return Optional.empty();
        }
        return new Optional<>(other.map(value));
    }
}
