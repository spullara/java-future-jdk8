package spullara.util;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
* Created with IntelliJ IDEA.
* User: sam
* Date: 3/21/13
* Time: 9:11 AM
* To change this template use File | Settings | File Templates.
*/
public class GatePredicate<T> implements Predicate<T> {

    private final Predicate<T> predicate;

    public GatePredicate(Predicate<T> predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean test(T t) {
        if (predicate.test(t)) {
            return true;
        }
        throw Gater.GateException.ONE;
    }
}
