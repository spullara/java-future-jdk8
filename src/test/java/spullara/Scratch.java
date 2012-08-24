package spullara;

import spullara.util.concurrent.Promise;

public class Scratch {
    interface Test<T, V> {
        T map(V v);
    }

    public static void main(String[] args) {
        Promise<String> promise5 = new Promise<>(new RuntimeException("Promise5"));
        promise5.map(v -> null);
    }
}

