package spullara;

import spullara.util.matching.Extractor;

import static spullara.util.Option.none;
import static spullara.util.Option.option;

public class Scratch {
    public static void main(String[] args) {
        Extractor<String, Integer> e = s -> {
            if (s.equals("1")) {
                return option(1);
            } else {
                return none();
            }
        };
    }
}

