package io.github.baifangkual.jlib.core.mark;

import io.github.baifangkual.jlib.core.lang.Indexed;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * <b>可迭代标记</b><br>
 * 类型实现该标记的{@link #iterator()}方法后，便可拥有系列方法行为，
 * 因该接口继承自 {@link Iterable}，遂可以使用语法糖 {@code for-each}<br>
 *
 * @author baifangkual
 * @see #into(Supplier)
 * @see #stream()
 * @see #parallelStream()
 * @see #toIndexedSpliterator(Spliterator, Indexed.FnBuildIndexed)
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
    @SuppressWarnings("NullableProblems")
    Iterator<T> iterator();


    /**
     * 将元素收集到指定集合
     * <p>“指定集合” 是函数提供的集合</p>
     * <pre>{@code
     * List<Long> list = Line.of(1L, 2L).into(ArrayList::new);
     * Set<Long> set = Line.of(1L, 2L).into(HashSet::new);
     * }</pre>
     *
     * @param fnGetCollect 集合提供函数
     * @return 集合
     * @throws NullPointerException 当给定的函数为空，或该类型返回的迭代器为空，或给定函数返回的集合实例为空时
     * @apiNote 因为该方法将元素收集到函数提供的集合中，遂函数生成的集合必须是可写的。
     * <p>除非该类型提供的迭代器会修改当前实例状态，否则该方法不会修改当前实例，仅会将当前实例中元素引用收集到指定集合</p>
     */
    default <Collect extends Collection<? super T>> Collect into(Supplier<Collect> fnGetCollect) {
        Objects.requireNonNull(fnGetCollect);
        Iterator<T> it = iterator();
        Objects.requireNonNull(it, "The Iterable.iterator() returned a null iterator");
        Collect collector = fnGetCollect.get();
        Objects.requireNonNull(collector, "fnGetCollect.get() returned a null collect");
        while (it.hasNext()) {
            T t = it.next();
            collector.add(t);
        }
        return collector;
    }

    /**
     * 返回{@link Stream}
     *
     * @return Stream
     * @apiNote 该默认方法会返回委托给List的流，若实现类中元素较少，应覆盖该实现已
     * 避免构建中间List的开销
     */
    default Stream<T> stream() {
        return into(ArrayList::new).stream();
    }

    /**
     * 返回并行{@link Stream}
     *
     * @return ParallelStream
     * @apiNote 该默认方法会返回委托给List的流，若实现类中元素较少，应覆盖该实现已
     * 避免构建中间List的开销
     */
    default Stream<T> parallelStream() {
        return stream().parallel();
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
     * 将给定 "带元素的" {@link Spliterator} 包装为 “带索引元素的” {@link Spliterator}
     * <p>"带索引元素" 由函数 {@link Indexed.FnBuildIndexed} 指定构造，
     * 该函数作用于 {@link Spliterator} 中的每个元素，该函数应为无副作用函数
     *
     * @param spliterator    {@link Spliterator}
     * @param fnBuildIndexed 函数-接收一个索引值和“元素”，返回一个“带索引的元素”
     * @param <T>            元素类型
     * @param <IndexedT>     带索引元素类型
     * @return “带索引元素的” {@link Spliterator}
     * @implNote 该方法实现参考 google guava(com.google.common.collect.Streams.mapWithIndex(...))
     * @apiNote 当该 {@link Spliterator} 被拆分时（{@link Spliterator#trySplit()}被回调）
     * 若原 {@link Spliterator} 的 {@link Spliterator#getExactSizeIfKnown()} 返回的 SIZE 总量大于 {@link Integer#MAX_VALUE}，
     * 则会抛出 {@link IllegalStateException}。
     * <p>返回的 IndexedSpliterator的 {@link Spliterator#characteristics()} 报告的 {@link Spliterator#SUBSIZED},
     * {@link Spliterator#SIZED},{@link Spliterator#ORDERED} 等取决于给定的 Spliterator 的 {@link Spliterator#characteristics()}。
     * <p>返回的 IndexedSpliterator 的状态合法性依赖于给定的 Spliterator 的状态合法性
     * @see Indexed#toIndexedStream(Stream)
     */
    static <T, IndexedT> Spliterator<IndexedT> toIndexedSpliterator(Spliterator<T> spliterator,
                                                                    Indexed.FnBuildIndexed<? super T, ? extends IndexedT> fnBuildIndexed) {
        Objects.requireNonNull(spliterator);
        Objects.requireNonNull(fnBuildIndexed);

        // 不知道拆分的子spliterator的大小的
        if (!spliterator.hasCharacteristics(Spliterator.SUBSIZED)) {
            Iterator<T> fromIterator = Spliterators.iterator(spliterator);
            return new Spliterators.AbstractSpliterator<>(
                    spliterator.estimateSize(),
                    spliterator.characteristics() & (Spliterator.ORDERED | Spliterator.SIZED)) {
                int index = 0;

                @Override
                public boolean tryAdvance(Consumer<? super IndexedT> action) {
                    if (fromIterator.hasNext()) {
                        action.accept(fnBuildIndexed.build(index++, fromIterator.next()));
                        return true;
                    }
                    return false;
                }
            };
        }
        // 知道拆分的子spliterator的大小的
        // 该参考自 google guava
        class SubSizedIndexedSpliterator implements Spliterator<IndexedT>, Consumer<T> {

            final Spliterator<T> fromSpliterator;
            int index;
            T holder;

            SubSizedIndexedSpliterator(Spliterator<T> sp, int index) {
                this.fromSpliterator = sp;
                this.index = index;
                this.holder = null;
            }

            SubSizedIndexedSpliterator of(Spliterator<T> from, int i) {
                return new SubSizedIndexedSpliterator(from, i);
            }

            @Override
            public void accept(T t) {
                this.holder = t;
            }

            @Override
            public boolean tryAdvance(Consumer<? super IndexedT> action) {
                // 这个原理大概明了：
                // 并行流走这里的，在 toIndexedStream 返回的流走终端操作时
                // 该方法被回调，从外界传递了一个 action
                // 当前的 IndexedSpliterator会调用自身持有的实际的 Stream<T> 的
                // spliterator，即 fromSpliterator引用的这个，
                // 该会先将自身（因为impl了 Consumer）传递到 fromSpliterator的tryAdvance中，
                // 而自身（Consumer）的accept是将传递的元素的引用放到holder引用位置
                // 然后在自己的tryAdvance的if实际的fromSpliterator返回true后
                // 调用外界传递过来的action，从holder和自身的index构建索引对象
                // 并调用外界传递的action，调用后的finally中再将holder置空
                // 我观察到 holder只在该方法内用了，那能否IndexedSpliterator自身不是先Consumer？
                // 然后 只在该方法内构建局部的Consumer？虽说也可，但不好：
                // 因为lambda无法修改外界引用，遂若要传递到内部fromSpliterator的action执行后能够拿到
                // fromSpliterator内的元素，则必须要用包装的引用类型比如atomicRef或自定义对象等... 没必要
                // 不得不说这样设计可谓厉害
                if (fromSpliterator.tryAdvance(this)) {
                    try {
                        // The cast is safe because tryAdvance puts a T into `holder`.
                        action.accept(fnBuildIndexed.build(index++, holder));
                        return true;
                    } finally {
                        holder = null;
                    }
                }
                return false;
            }

            @Override
            public SubSizedIndexedSpliterator trySplit() {
                Spliterator<T> splitOrNull = fromSpliterator.trySplit();
                if (splitOrNull == null) {
                    return null;
                }
                SubSizedIndexedSpliterator result = of(splitOrNull, index);
                long exactSizeIfKnown = splitOrNull.getExactSizeIfKnown();
                // fix or：拆出来的太大了，因为 FnBuildIndexed 和默认的 Indexed 的
                // 索引都是int，当其大于最大int值，则证明元素过多已无法表达了
                // 遂此处直接抛出异常
                if (exactSizeIfKnown > Integer.MAX_VALUE) {
                    throw new IllegalStateException("Exact sized split is too large");
                }
                this.index += (int) exactSizeIfKnown;
                return result;
            }

            @Override
            public long estimateSize() {
                return fromSpliterator.estimateSize();
            }

            @Override
            public int characteristics() {
                return fromSpliterator.characteristics()
                       & (Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED);
            }

        }
        return new SubSizedIndexedSpliterator(spliterator, 0);
    }

    /**
     * 携带被迭代元素索引的迭代器
     * <p>索引从 {@code 0} 开始，表示从迭代器被迭代到的元素的顺序，
     * {@link #remove()} 方法将会委托至原迭代器，删除元素不会导致索引回溯
     *
     * @param <T>        不带索引的被迭代类型
     * @param <IndexedT> 包含索引的类型
     */
    class IndexedIter<T, IndexedT> implements Iterator<IndexedT> {

        private final Iterator<T> it;
        private final Indexed.FnBuildIndexed<? super T, ? extends IndexedT> fnIndexed;
        private int index;

        private IndexedIter(Iterator<T> it,
                            Indexed.FnBuildIndexed<? super T, ? extends IndexedT> fnIndexed) {
            Objects.requireNonNull(it, "Iterator");
            Objects.requireNonNull(fnIndexed, "idxFn2");
            this.index = 0;
            this.it = it;
            this.fnIndexed = fnIndexed;
        }

        public static <T, IndexedT> IndexedIter<T, IndexedT> of(Iterator<T> it,
                                                                Indexed.FnBuildIndexed<? super T, ? extends IndexedT> fnIndexed) {
            return new IndexedIter<>(it, fnIndexed);
        }


        public static <T> Iterator<Indexed<T>> ofIndexed(Iterator<T> it) {
            return of(it, Indexed.FnBuildIndexed.fnBuildIndexed());
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public IndexedT next() {
            return fnIndexed.build(index++, it.next());
        }

        @Override
        public void remove() {
            it.remove();
        }
    }


    /**
     * 类型映射代理迭代器(element type mapping proxy iterator)<br>
     * 给定一个迭代器和一个函数以构造该类型，每次调用迭代器的{@link Iterator#next()}方法时，
     * 都会对元素使用函数进行类型转换，{@link #remove()} 方法将会委托至原迭代器
     *
     * @param <E> 映射前类型
     * @param <T> 映射后类型
     */
    class MappedIter<E, T> implements Iterator<T> {
        private final Iterator<E> it;
        private final Function<? super E, ? extends T> fn;

        private MappedIter(Iterator<E> it,
                           Function<? super E, ? extends T> fnMapping) {
            this.it = Objects.requireNonNull(it, "it(Iterator) is null");
            this.fn = Objects.requireNonNull(fnMapping, "fnMapping(Function) is null");
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
            return new MappedIter<>(it, fn);
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public T next() {
            // 不需要阻止 null 进入函数，因为函数提供者可能准许 null 并返回默认值，也可能被包装的迭代器不会有null
            return fn.apply(it.next());
        }

        @Override
        public void remove() {
            it.remove();
        }
    }
}
