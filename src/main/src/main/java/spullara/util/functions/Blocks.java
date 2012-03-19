package spullara.util.functions;

public class Blocks {
    public static <T> Block<T> chain(final Block<T> block1, final Block<T> block2) {
        return new Block<T>() {
            @Override
            public void apply(T t) {
                block1.apply(t);
                block2.apply(t);
            }
        };
    }
}
