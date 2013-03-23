package spullara;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.Streams;

public class Scratch {

    static String address(InetSocketAddress sa) {
        return elvis(elvis(sa, InetSocketAddress::getAddress), InetAddress::getHostAddress);
    }

    public static <T, V> V elvis(T t, Function<T, V> mapper) {
        return t == null ? null : mapper.apply(t);
    }

    public static void test() {
        Map<String, Object> map = new HashMap<>();
        map.put("test", 1);


        List<String> ss = new ArrayList<>();
        ss.stream().flatMap((String x, Consumer<String> sink) -> {
            for (String s : x.split(",")) {
                sink.accept(s);
            }
        });
    }

    @Test
    public static void testStreams() {

        IntStream map = Streams.zip(
                Streams.intRange(0, 100).boxed(),
                Streams.intRange(0, 100).boxed(),
                Math::multiplyExact)
                .map((ToIntFunction<Integer>) i -> i);

    }
}
