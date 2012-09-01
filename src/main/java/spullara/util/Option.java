package spullara.util;

import java.util.ArrayList;
import java.util.functions.Mapper;

public abstract class Option<T> extends ArrayList<T> {
    private static class None<T> extends Option<T> {
        private None() {
            super(0);
        }
    }

    protected Option(int i) {
        super(i);
    }

    private static final Option NONE = new None();

    public static <T> Option<T> none() {
        return NONE;
    }

    @Override
    public <U> Option<U> map(Mapper<? super T, ? extends U> mapper) {
        Iterable<U> map = super.map(mapper);
        if (map.isEmpty()) {
            return NONE;
        } else {
            return option(map.getOnly());
        }
    }

    public static <T> Option<T> option(T value) {
        if (value == null) {
            return NONE;
        } else {
            return new Some<T>(value);
        }
    }
}

