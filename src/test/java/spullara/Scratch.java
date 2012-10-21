package spullara;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.functions.Mapper;

import static java.util.streams.Streams.stream;

public class Scratch {

    static String address(InetSocketAddress sa) {
        return elvis(elvis(sa, isa -> isa.getAddress()), ia -> ia.getHostAddress());
    }

    public static <T, V> V elvis(T t, Mapper<T, V> mapper) {
        return t == null ? null : mapper.map(t);
    }

    private static final Iterable empty = new ArrayList();

    @SuppressWarnings("unchecked")
    public static <T> Iterable<T> ni(Iterable<T> iterable) {
        return iterable == null ? empty : iterable;
    }

    public static void m(Iterable<String> strings) {
        for (String s : ni(strings)) {

        }
        stream(ni(strings)).forEach((string) -> {

        });
    }
}

