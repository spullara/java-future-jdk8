package spullara.util;

/**
 * Curries method calls.
 */
public class Currier {

    public interface C2<T, U, V> {
        T invoke(U u, V v);
    }

    public interface C1<T, U> {
        T invoke(U u);
    }

    public interface C0<T> {
        T invoke();
    }

    public static <T, U> Currier.C0<T> curry(C1<T, U> get, U u) {
        return () -> get.invoke(u);
    }

    public static <T, U, V> Currier.C0<T> curry(C2<T, U, V> get, U u, V v) {
        return () -> get.invoke(u, v);
    }

    public static <T, U, V> Currier.C1<T, V> curry(C2<T, U, V> get, U u) {
        return (v) -> get.invoke(u, v);
    }
}
