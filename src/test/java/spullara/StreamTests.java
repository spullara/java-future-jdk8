package spullara;

import org.junit.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Integer.parseInt;
import static java.util.stream.Collectors.groupingBy;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

/**
 * TODO: Edit this
 * <p/>
 * User: sam
 * Date: 2/9/13
 * Time: 10:57 AM
 */
public class StreamTests {
    @Test
    public void testTransformValues1() {
        Map<String, String> numbers = new HashMap<>();
        numbers.put("one", "1");
        numbers.put("two", "2");
        numbers.put("three", "3");
        numbers.put("four", "4");
        Map<String, Integer> collect = numbers.entrySet().stream().collect(
                HashMap::new,
                (map, entry) ->  map.put(entry.getKey(), parseInt(entry.getValue())),
                Map::putAll
        );
        assertEquals(1, collect.get("one").intValue());
    }

    @Test
    public void testTransformValues2() {
        Map<String, String> numbers = new HashMap<>();
        numbers.put("one", "1");
        numbers.put("two", "2");
        numbers.put("three", "3");
        numbers.put("four", "4");
        Map<String, Integer> collect = numbers.entrySet().parallelStream().collect(
                HashMap::new,
                (map, entry) ->  map.put(entry.getKey(), parseInt(entry.getValue())),
                Map::putAll
        );
        assertEquals(1, collect.get("one").intValue());


    }

    @Test
    public void testGroupingBy() {
        Map<String, String> numbers = new HashMap<>();
        numbers.put("one", "1");
        numbers.put("two", "2");
        numbers.put("three", "3");
        numbers.put("four", "4");
        Map<Boolean, Collection<Map.Entry<String, String>>> collect = numbers.entrySet().stream().collect(
                groupingBy((entry) -> parseInt(entry.getValue()) <= 2)
        );
        assertEquals(2, collect.get(true).size());
        assertEquals(2, collect.get(false).size());
    }

    @Test
    public void testComputeIfAbsent() {
        Map<String, String> numbers = new HashMap<>();
        numbers.put("one", "1");
        numbers.put("two", "2");
        numbers.put("three", "3");
        numbers.put("four", "4");
        numbers.computeIfAbsent("five", (k) -> "5");
        numbers.computeIfAbsent("six", (k) -> null);
        assertFalse(numbers.containsKey("six"));
    }
}
