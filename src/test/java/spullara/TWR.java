package spullara;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by sam on 6/24/13.
 */
public class TWR {
    public static <T extends AutoCloseable, R> void autoclose(Supplier<T> supplier, Consumer<T> consumer) throws Exception {
        T autoCloseable = supplier.get();
        try {
            consumer.accept(autoCloseable);
        } finally {
            autoCloseable.close();
        }
    }
}
