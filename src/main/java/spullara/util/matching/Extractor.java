package spullara.util.matching;

import java.util.Optional;

/**
* Created with IntelliJ IDEA.
* User: spullara
* Date: 8/31/12
* Time: 12:14 PM
* To change this template use File | Settings | File Templates.
*/
public interface Extractor<T, W> {
    Optional<W> unapply(T t);
}
