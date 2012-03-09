package spullara.util.concurrent;

public class Tuple2<T, V> {
    public final T _1;
    public final V _2;

    public Tuple2(T t, V v) {
	this._1 = t;
	this._2 = v;
    }

}