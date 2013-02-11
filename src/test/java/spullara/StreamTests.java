package spullara;

import org.junit.Test;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.lang.Integer.parseInt;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.groupingReduce;
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
        Map<String, Integer> collect = transformValues(numbers, Integer::parseInt);
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

    static class Row {
        Row(String region, String gender, String style, int units, double price) {
            this.region = region;
            this.gender = gender;
            this.style = style;
            this.units = units;
            this.price = price;
        }
        final String region;
        final String gender;
        final String style;
        final int units;
        final double price;

        @Override
        public String toString() {
            return "Row{" +
                    "gender='" + gender + '\'' +
                    ", region='" + region + '\'' +
                    ", style='" + style + '\'' +
                    ", units=" + units +
                    ", price=" + price +
                    '}';
        }
    }

    @Test
    public void testPivotTable() {
        List<Row> rows = new ArrayList<>();
        rows.add(new Row("East", "Boy", "Tee", 10, 12.00));
        rows.add(new Row("East", "Boy", "Golf", 15, 20.00));
        rows.add(new Row("East", "Girl", "Tee", 8, 14.00));
        rows.add(new Row("East", "Girl", "Golf", 20, 24.00));
        rows.add(new Row("West", "Boy", "Tee", 5, 12.00));
        rows.add(new Row("West", "Boy", "Golf", 12, 20.00));
        rows.add(new Row("West", "Girl", "Tee", 15, 14.00));
        rows.add(new Row("West", "Girl", "Golf", 10, 24.00));

        // groupBy region and gender, summing total sales
        Map<String, Map<String, Double>> pivot = rows.stream().collect(groupingBy(r -> r.region,
                groupingReduce(r -> r.gender, r -> r.price * r.units, Double::sum)));
        System.out.println(pivot);
    }

    public static <K, V, W> Map<K, W> transformValues(Map<K, V> oldmap, Function<V, W> transform) {
        return oldmap.entrySet().stream().collect(HashMap::new,
                (map, entry) -> map.put(entry.getKey(), transform.apply(entry.getValue())),
                Map::putAll);
    }

    public static <K1, K2, IN, OUT> Map<K1, Map<K2, OUT>> pivot(Collection<IN> rows, Function<IN, K1> left, Function<IN, K2> top, Function<Collection<IN>, OUT> aggregate) {
        Map<K1, Collection<IN>> leftGroup = rows.stream().collect(groupingBy(left));
        Map<K1, Map<K2, Collection<IN>>> grid = transformValues(leftGroup, (rs) -> rs.stream().collect(groupingBy(top)));
        return transformValues(grid, (map) -> transformValues(map, aggregate));
    }
}
