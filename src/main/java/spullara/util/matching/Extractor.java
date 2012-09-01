package spullara.util.matching;

import spullara.util.Option;

import java.util.functions.Mapper;

/**
* Created with IntelliJ IDEA.
* User: spullara
* Date: 8/31/12
* Time: 12:14 PM
* To change this template use File | Settings | File Templates.
*/
public interface Extractor<T, W> {
    Option<W> unapply(T t);
}
