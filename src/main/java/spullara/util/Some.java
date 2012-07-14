package spullara.util;

/**
 * Represents an optional value that exists.
 */
public class Some<T> extends Option<T> {

    protected Some(T value) {
        this();
        add(value);
    }

    protected Some() {
        super(1);
    }

}
