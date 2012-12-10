package spullara.util;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static spullara.util.Currier.curry;

public class CurrierTest {
    @Test
    public void curryOneToZero() {
        Map<String, Integer> map = new HashMap<>();
        map.put("test", 1);
        Currier.C0<Integer> c0 = curry(map::get, "test");
        assertEquals(1, c0.invoke().intValue());
    }

    @Test
    public void curryTwoToZero() {
        Map<String, Integer> map = new HashMap<>();
        Currier.C0<Integer> c0 = curry(map::put, "test", 1);
        c0.invoke();
        assertEquals(1, map.get("test").intValue());
    }

    @Test
    public void curryTwoToOne() {
        Map<String, Integer> map = new HashMap<>();
        Currier.C1<Integer, Integer> c1 = curry(map::put, "test");
        c1.invoke(1);
        assertEquals(1, map.get("test").intValue());
    }
}
