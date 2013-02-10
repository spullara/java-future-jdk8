package spullara;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Scratch {

    static String address(InetSocketAddress sa) {
        return elvis(elvis(sa, isa -> isa.getAddress()), InetAddress::getHostAddress);
    }

    public static <T, V> V elvis(T t, Function<T, V> mapper) {
        return t == null ? null : mapper.apply(t);
    }


    public static void test() {
        Map<String, Object> map = new HashMap<>();
        map.put("test", 1);

    }
}

