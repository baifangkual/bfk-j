package io.github.baifangkual.bfk.j.mod.core.lang;

import io.github.baifangkual.bfk.j.mod.core.mark.Iter;
import io.github.baifangkual.bfk.j.mod.core.trait.Cloneable;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * <b>带索引的包装</b>
 * <p>该实例包装一个对象T（可以为 {@code null})，返回 {@code Indexed<T>}，
 * 以表示对象的索引值。该实例不可变
 * <p>类型 {@link Tup2} 无法表达该类型效果，因为其无法表达空指针引用 {@code null}
 * <pre>{@code
 * Stream<T> s = Stream.of(...);
 * Stream<Indexed<T>> is = Indexed.toIndexedStream(s);
 * List<T> l = List.of(...);
 * List<Indexed<T>> il = Indexed.toIndexedList(l);
 * T t = ...;
 * Indexed<T> indexedT = Indexed.of(0, t);
 * Assert.eq(t, indexedT.value());
 * Assert.eq(0, indexedT.index());
 * Indexed<T> indexedT2 = Indexed.of(1, null);
 * Assert.isNull(indexedT2.value());
 * Assert.optionalIsEmpty(indexedT2.tryValue());
 * }</pre>
 *
 * @param <T> 元素类型
 * @author baifangkual
 * @implNote 为什么 index 不是 long 类型的？因为我觉得 int 就够用了，
 * 除非无限流，不然内存应该塞不下...
 * @see #toIndexedList(List)
 * @see #toIndexedStream(Stream)
 * @see #toIndexedStream(Stream, FnBuildIndexed)
 * @see FnBuildIndexed
 * @since 2025/6/2 v0.0.7
 */
public final class Indexed<T> implements Serializable, Cloneable<Indexed<T>> {

    private final int index;
    private final T value;

    Indexed(int index, T value) {
        this.index = index;
        this.value = value;
    }

    public static <T> Indexed<T> of(int index, T value) {
        return new Indexed<>(index, value);
    }

    /**
     * 获取索引值
     *
     * @return 索引
     */
    public int index() {
        return index;
    }

    /**
     * 获取实例值，可能为 {@code null}
     *
     * @return 实例值（nullable）
     * @see #tryValue()
     */
    public T value() {
        return value;
    }

    /**
     * 实例值是否为 {@code null}
     *
     * @return true 为null，反之则不为null
     */
    public boolean isNullValue() {
        return value == null;
    }

    /**
     * 获取实例值 (wrap of Optional)
     *
     * @return 实例值 (wrap of Optional)
     */
    public Optional<T> tryValue() {
        return Optional.ofNullable(value);
    }

    public <V> Indexed<V> map(Function<? super T, ? extends V> fn) {
        return Indexed.of(index, fn.apply(value));
    }

    public <V> Indexed<V> flatMap(Function<? super T, ? extends Indexed<? extends V>> fn) {
        @SuppressWarnings("unchecked")
        Indexed<V> ni = (Indexed<V>) fn.apply(value);
        return ni; // 不检查可能为null的情况
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public Indexed<T> clone() {
        return Indexed.of(this.index, this.value);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Indexed<?> indexed)) return false;
        return index == indexed.index && Objects.equals(value, indexed.value);
    }

    @Override
    public int hashCode() {
        int result = index;
        result = 31 * result + Objects.hashCode(value);
        return result;
    }

    @Override
    public String toString() {
        return "(" + index + "): " + value;
    }

    /**
     * 函数-给定索引和实体，生成带索引的实例
     * <pre>{@code
     * T t = ...;
     * FnBuildIndexed<T, Indexed<T>> fn = Indexed::of;
     * Indexed<T> indexedT = fn.buildIndexed(0, t);
     * }</pre>
     *
     * @param <T>        实例
     * @param <IndexedT> 带索引的实例 (比如 {@link Indexed})
     */
    @FunctionalInterface
    public
    interface FnBuildIndexed<T, IndexedT> {
        IndexedT buildIndexed(int index, T t);
    }


    /**
     * 函数-无副作用，默认使用 {@link Indexed} 类型表示带索引的实例
     *
     * @param <T> 需表达索引的实体
     * @return 默认函数
     */
    public static <T> FnBuildIndexed<T, Indexed<T>> fnDefaultBuildIndexed() {
        return Indexed::of;
    }

    /**
     * 读取给定List中元素，返回UnmodifiedList，其中元素为 {@link Indexed} 带索引的原元素
     * <pre>{@code
     * List<T> l = List.of(...);
     * // t1, t2, t3
     * List<Indexed<T>> il = Indexed.toIndexedList(l);
     * // (0, t1), (1, t2), (2, t3)
     * for (Indexed<T> it : il) {
     *     Assert.isTrue(it.value() == l.get(it.index()));
     * }
     * }</pre>
     *
     * @param list 列表
     * @param <T>  列表中元素
     * @return UnmodifiedList
     * @apiNote 该方法仅接收List，因为Collection中Set等无序，索引其中元素无意义
     */
    public static <T> List<Indexed<T>> toIndexedList(List<? extends T> list) {
        Objects.requireNonNull(list, "list");
        if (list.isEmpty()) return Collections.emptyList();

        // 针对 RandomAccess 列表使用索引访问 （ArrayList, Vector 等）
        if (list instanceof RandomAccess) {
            return IntStream.range(0, list.size())
                    .mapToObj(i -> Indexed.of(i, (T) list.get(i)))
                    .toList();
        } else {
            // 针对 LinkedList 等无法随机访问的 先变为indexedStream
            // 否则 其的get(idx) 方法时间复杂度O(n)消受不起
            Stream<Indexed<T>> indexedStream = toIndexedStream(list.stream());
            return indexedStream.toList();
        }
    }

    /**
     * 将给定‘元素流’转为‘带索引元素流’，顺序等属性与原流一致
     *
     * <p>在 java {@link Stream} 体系中，这是一个 中间操作
     *
     * @param stream 元素流
     * @param <T>    原元素流中元素类型
     * @return 带索引元素流 {@code Stream<Indexed<T>>}
     * @see #toIndexedStream(Stream, FnBuildIndexed)
     */
    public static <T> Stream<Indexed<T>> toIndexedStream(Stream<? extends T> stream) {
        return toIndexedStream(stream, Indexed.fnDefaultBuildIndexed());
    }

    /**
     * 将给定‘元素流’转为‘带索引元素流’，顺序等属性与原流一致，
     * 其中‘带索引元素流’中‘带索引元素’由函数 {@link FnBuildIndexed} 指定构造
     * <p>在 java {@link Stream} 体系中，这是一个 中间操作
     *
     * @param stream    元素流
     * @param fnIndexed 函数-接收一个索引值和元素，返回一个‘带索引的元素’
     * @param <T>       原元素流中元素类型
     * @param <R>       带索引元素流中带索引元素类型
     * @return 带索引元素流
     * @implNote 该方法实现参考 google guava(com.google.common.collect.Streams#mapWithIndex(...))
     * @see #toIndexedStream(Stream)
     * @see Iter#toIndexedSpliterator(Spliterator, FnBuildIndexed)
     */
    public static <T, R> Stream<R> toIndexedStream(Stream<T> stream,
                                                   FnBuildIndexed<? super T, ? extends R> fnIndexed) {
        Objects.requireNonNull(stream);
        Objects.requireNonNull(fnIndexed);
        boolean isParallel = stream.isParallel();
        Spliterator<T> fromSpliterator = stream.spliterator();
        Spliterator<R> indexedSpliterator = Iter.toIndexedSpliterator(fromSpliterator, fnIndexed);
        return StreamSupport.stream(indexedSpliterator, isParallel).onClose(stream::close);
    }

}
