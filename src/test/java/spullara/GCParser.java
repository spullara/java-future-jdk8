package spullara;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GCParser {
    public static void main(String[] args) throws FileNotFoundException {
        Pattern memory = Pattern.compile("([0-9]+)K->([0-9]+)K");
        long total = new BufferedReader(new FileReader(args[0])).lines().mapToLong(line -> {
            Matcher m = memory.matcher(line);
            if (m.find()) {
                long start = Long.parseLong(m.group(1));
                long end = Long.parseLong(m.group(2));
                return start - end;
            }
            return 0;
        }).sum();
        System.out.println(total);
    }
}
