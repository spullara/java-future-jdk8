package spullara;

import org.junit.Test;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Streams;

import static java.lang.Integer.parseInt;
import static java.util.stream.Collectors.*;
import static java.util.stream.Collectors.groupingBy;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static spullara.util.Limiter.limit;
import static spullara.util.Limiter.substream;

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
                (map, entry) -> map.put(entry.getKey(), parseInt(entry.getValue())),
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
        Map<Boolean, List<Map.Entry<String, String>>> collect = numbers.entrySet().stream().collect(
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
        Map<String, Map<String, DoubleSummaryStatistics>> pivot = rows.stream().collect(
                groupingBy(r -> r.region,
                        groupingBy(r -> r.gender,
                                toDoubleSummaryStatistics(r -> r.price * r.units))));
        System.out.println(pivot);
    }

    public static <K, V, W> Map<K, W> transformValues(Map<K, V> oldmap, Function<V, W> transform) {
        return oldmap.entrySet().stream().collect(HashMap<K, W>::new,
                (map, entry) -> map.put(entry.getKey(), transform.apply(entry.getValue())),
                Map::putAll);
    }

    List<Integer> intStream = Streams.intRange(1, 100).boxed().collect(toList());

    @Test
    public void testGater() {
        List<String> list = substream(limit(intStream.stream()
                .peek(System.out::println)
                , t -> t < 11)
                , t -> t < 8)
                .map(t -> "Num: " + t)
                .collect(toList());
        assertEquals(Arrays.asList("Num: 8", "Num: 9", "Num: 10"), list);
        list = substream(intStream.stream()
                .peek(System.out::println)
                , t -> t < 8
                , t -> t < 11)
                .map(t -> "Num: " + t)
                .collect(toList());
        assertEquals(Arrays.asList("Num: 8", "Num: 9", "Num: 10"), list);
    }
}
