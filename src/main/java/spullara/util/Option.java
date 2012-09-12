package spullara.util;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.functions.Mapper;

public abstract class Option<T> extends AbstractList<T> {
    private static class None<T> extends Option<T> {
        private None() {
            super();
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public T get(int index) {
            throw new ArrayIndexOutOfBoundsException("None has zero elements");
        }
    }

    private static final Option NONE = new None();

    public static <T> Option<T> none() {
        return NONE;
    }

    public static <T> Option<T> none(Class<T> c) {
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

