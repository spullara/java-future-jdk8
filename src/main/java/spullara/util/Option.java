package spullara.util;

import java.util.ArrayList;

public abstract class Option<T> extends ArrayList<T> {
    private static class None<T> extends Option<T> {
        private None() {
            super(0);
        }
    }

    protected Option(int i) {
        super(i);
    }

    public static final Option NONE = new None();

    public static <T> Option<T> option(T value) {
        if (value == null) {
            return NONE;
        } else {
            return new Some<T>(value);
        }
    }
}

