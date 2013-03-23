package spullara.util;

import java.util.function.Predicate;

public class Gater {
    static class GateException extends RuntimeException {
        static GateException ONE = new GateException();
        private GateException() {}
    }

    public static void gated(Runnable runnable) {
        try {
            runnable.run();
        } catch (GateException ge) {
            // ignore
        }
    }

    public static <T> GatePredicate<T> gate(Predicate<T> predicate) {
        return new GatePredicate<>(predicate);
    }
}
