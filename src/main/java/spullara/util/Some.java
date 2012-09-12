package spullara.util;

/**
 * Represents an optional value that exists.
 */
public class Some<T> extends Option<T> {

    private final T t;

    protected Some(T value) {
        t = value;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public T get(int index) {
        if (index == 0) {
            return t;
        } else {
            throw new ArrayIndexOutOfBoundsException("Some() has a single element");
        }
    }

}
