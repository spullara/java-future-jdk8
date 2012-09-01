package spullara.util.matching;

import spullara.util.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.functions.Mapper;

import static spullara.util.Option.option;

public class Match<T, V> {

    private List<Extractor> extractors = new ArrayList<>();
    private List<Mapper> matched = new ArrayList<>();
    private Mapper other;

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

    public Match<T, V> orElse(Mapper<T, V> other) {
        this.other = other;
        return this;
    }

    public Option<V> check(T value) {
        for (int i = 0; i < extractors.size(); i++) {
            Option isMatched = extractors.get(i).unapply(value);
            if (isMatched != null && !isMatched.isEmpty()) {
                return (Option<V>) option(matched.get(i).map(isMatched.getOnly()));
            }
        }
        if (other == null) {
            return option(null);
        }
        return (Option<V>) option(other.map(value));
    }
}
