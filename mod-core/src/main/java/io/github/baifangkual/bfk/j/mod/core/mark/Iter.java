package io.github.baifangkual.bfk.j.mod.core.mark;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * <b>可迭代标记接口</b><br>
 * 类型实现该标记的{@link #iterator()}方法后，便可拥有系列方法行为，可
 *
 * @author baifangkual
 * @since 2025/5/12 v0.0.4
 */
public interface Iter<T> extends Iterable<T> {

    /**
     * 迭代器<br>
     * 多次调用该方法返回的迭代器并非同一个迭代器
     *
     * @return 迭代器
     */
    @Override
    Iterator<T> iterator();


    /**
     * 执行给定的集合提供者函数获取函数返回的{@link Set}，并收集该类型当中的所有元素<br>
     * 返回的{@link Set}为给定的集合提供者函数返回的{@link Set}
     *
     * @param fn 集合提供者函数
     * @return Set
     * @throws NullPointerException 当给定的函数为空时，或该类型返回的迭代器为空时，或给定函数返回的Set为空时
     * @apiNote 因为该方法将元素收集到函数生成的集合中，遂函数生成的集合必须是可写的
     */
    default Set<T> toSet(Supplier<? extends Set<T>> fn) {
        Objects.requireNonNull(fn, "fn is null");
        Iterator<T> it = iterator();
        Objects.requireNonNull(it, "The Iterable.iterator() returned a null iterator");
        Set<T> set = fn.get();
        Objects.requireNonNull(set, "fn.get() returned a null Set");
        while (it.hasNext()) {
            T t = it.next();
            set.add(t);
        }
        return set;
    }


    /**
     * 执行给定的集合提供者函数获取函数返回的{@link List}，并收集该类型当中的所有元素<br>
     * 返回的{@link List}为给定的集合提供者函数返回的{@link List}
     *
     * @param fn 集合提供者函数
     * @return List
     * @throws NullPointerException 当给定的函数为空时，或该类型返回的迭代器为空时，或给定函数返回的List为空时
     * @apiNote 因为该方法将元素收集到函数生成的集合中，遂函数生成的集合必须是可写的
     */
    default List<T> toList(Supplier<? extends List<T>> fn) {
        Objects.requireNonNull(fn, "fn is null");
        Iterator<T> it = iterator();
        Objects.requireNonNull(it, "The Iterable.iterator() returned a null iterator");
        List<T> list = fn.get();
        Objects.requireNonNull(list, "fn.get() returned a null list");
        while (it.hasNext()) {
            T t = it.next();
            list.add(t);
        }
        return list;
    }

    /**
     * 返回{@link Stream}<br>
     * 该方法返回的流实际是委托给{@link ArrayList}返回的流，
     * 并非默认{@link Iterable#spliterator()}构造的流，遂不会差性能
     *
     * @return Stream
     * @apiNote 该默认方法会返回委托给List的流，若实现类中元素较少，应覆盖该实现已
     * 避免构建中间List的开销
     */
    default Stream<T> stream() {
        return toList(ArrayList::new).stream();
    }

    /**
     * 返回并行{@link Stream}<br>
     * 该方法返回的流实际是委托给{@link ArrayList}返回的流，
     * 并非默认{@link Iterable#spliterator()}构造的流，遂不会差性能
     *
     * @return ParallelStream
     * @apiNote 该默认方法会返回委托给List的流，若实现类中元素较少，应覆盖该实现已
     * 避免构建中间List的开销
     */
    default Stream<T> parallelStream() {
        return toList(ArrayList::new).parallelStream();
    }

    // static fn ----------------------------------------------------------

    /**
     * 给定一个可迭代对象，将其转为{@link Stream}<br>
     * 该方法入参的可迭代对象当为集合类型时，将使用集合类型的{@link Collection#stream()}系列方法，
     * 因为集合类型的系列方法时经过特化的，若非集合类型，则使用默认的{@link Iterable#spliterator()}，
     * 性能将取决于{@link Spliterator}实现
     *
     * @param it       可迭代对象
     * @param parallel 是否并行
     * @param <T>      元素类型
     * @return Stream | ParallelStream
     * @throws NullPointerException 当给定的可迭代对象为空时
     */
    static <T> Stream<T> toStream(Iterable<T> it, boolean parallel) {
        Objects.requireNonNull(it, "it(Iterable) is null");
        if (it instanceof Collection<T> coll) {
            if (parallel) {
                return coll.parallelStream();
            } else {
                return coll.stream();
            }
        }
        return StreamSupport.stream(it.spliterator(), parallel);
    }

    /**
     * 给定一个可迭代对象，将其转为{@link Stream}<br>
     * 该方法入参的可迭代对象当为集合类型时，将使用集合类型的{@link Collection#stream()}系列方法，
     * 因为集合类型的系列方法时经过特化的，若非集合类型，则使用默认的{@link Iterable#spliterator()}，
     * 性能将取决于{@link Spliterator}实现
     *
     * @param it  可迭代对象
     * @param <T> 元素类型
     * @return Stream
     * @throws NullPointerException 当给定的可迭代对象为空时
     */
    static <T> Stream<T> toStream(Iterable<T> it) {
        return toStream(it, false);
    }

    /**
     * 给定一个迭代器，将其转为{@link Stream}<br>
     * 该方法将使用{@link Spliterators#spliteratorUnknownSize(Iterator, int)}生成{@link Spliterator}来生成{@link Stream}，
     * 默认实现返回的 {@link Spliterator} 的拆分能力较差，未调整大小，并且不报告任何自身特征
     *
     * @param it       迭代器
     * @param parallel 是否并行
     * @param <T>      元素类型
     * @return Stream | ParallelStream
     * @throws NullPointerException 当给定的迭代器为空时
     */
    static <T> Stream<T> toStream(Iterator<T> it, boolean parallel) {
        Objects.requireNonNull(it, "it(Iterator) is null");
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, 0), parallel);
    }

    /**
     * 给定一个迭代器，将其转为{@link Stream}
     * 该方法将使用{@link Spliterators#spliteratorUnknownSize(Iterator, int)}生成{@link Spliterator}来生成{@link Stream}，
     * 默认实现返回的 {@link Spliterator} 的拆分能力较差，未调整大小，并且不报告任何自身特征
     *
     * @param it  迭代器
     * @param <T> 元素类型
     * @return Stream
     * @throws NullPointerException 当给定的迭代器为空时
     */
    static <T> Stream<T> toStream(Iterator<T> it) {
        return toStream(it, false);
    }


    // static fn ----------------------------------------------------------

    /**
     * 返回用于遍历和分区源元素的对象{@link Spliterator}<br>
     * 该方法默认委托给{@link Iterable#spliterator()}，该被委托的方法是低效的，
     * 实现类中元素若较多，应重写该方法，或委托至经过特化的java.List
     *
     * @return {@link Spliterator}
     * @see Iterable#spliterator()
     */
    @Override
    default Spliterator<T> spliterator() {
        return Iterable.super.spliterator();
    }


    /**
     * 类型映射迭代器装饰器(element type mapping iterator decorator)<br>
     * 给定一个迭代器和一个函数以构造该类型，每次调用迭代器的{@link Iterator#next()}方法时，
     * 都会对元素使用函数进行类型转换
     *
     * @param <E> 映射前类型
     * @param <T> 映射后类型
     */
    class ETMIterDecorator<E, T> implements Iterator<T> {
        private final Iterator<E> it;
        private final Function<? super E, ? extends T> fn;

        private ETMIterDecorator(Iterator<E> it,
                                 Function<? super E, ? extends T> fn) {
            this.it = Objects.requireNonNull(it, "it(Iterator) is null");
            this.fn = Objects.requireNonNull(fn, "fn(Function) is null");
        }

        /**
         * 创建一个类型映射迭代器装饰器<br>
         * 给定的函数在{@link Iterator#next()}方法内被调用
         *
         * @param it  迭代器
         * @param fn  函数，映射迭代器中元素
         * @param <E> 映射前类型
         * @param <T> 映射后类型
         * @return 迭代器
         */
        public static <E, T> Iterator<T> of(Iterator<E> it,
                                            Function<? super E, ? extends T> fn) {
            return new ETMIterDecorator<>(it, fn);
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public T next() {
            return fn.apply(it.next());
        }

        @Override
        public void remove() {
            it.remove();
        }
    }
}
