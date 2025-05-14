package io.github.baifangkual.bfk.j.mod.core.model;

import io.github.baifangkual.bfk.j.mod.core.exception.ResultUnwrapException;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * <b>结果（Result）</b><br>
 * 一个不可变容器 线程安全，表达可能操作失败的逻辑的结果，
 * 该类型实例要么为“正常结果”({@link R.Ok})携带“正常结果值”({@link T})，
 * 要么为“错误结果”({@link R.Err})携带“错误结果值”({@link E})，
 * 该类内缺省主语的方法签名主语默认形容的是{@link T}<br>
 * 进行某操作后返回该类型实例，若操作成功，则{@link #isOk()}为true，{@link #isErr()}为false,
 * 调用{@link #ok()}将返回“正常结果”，而调用{@link #err()}则抛出异常，
 * 若操作失败，则{@link #isOk()}为false，{@link #isErr()}为true,
 * 调用{@link #err()}将返回“错误结果”，而调用{@link #ok()}则抛出异常<br>
 * 类似{@link Optional}约束的那样，对该类型的引用应当永远不为{@code null}<br>
 *
 * @param <T> 正确结果值类型
 * @param <E> 错误结果值类型
 * @author baifangkual
 * @apiNote {@link E}并未约束一定为{@link Exception}类型的子级，换句话说，{@link R}携带的“错误结果值”并不一定是java异常，
 * 使用方应确保自身代码中所有“错误结果值”为{@link Exception}类型及其子级的{@link R}对象都最终被解包处理了，否则可能造成部分java异常被静默丢弃。
 * @implNote 该类型的实现参考Rust语言中Result类型的行为和使用场景，抽象的讲，
 * 即想要在java语言中避免try-cache-throw这种单独的异常信息传输通道（对于一个执行可能产生异常的方法（尤其是运行时异常）），
 * 其方法声明形如{@code T method(...)}，这种方法声明从外界使用者看来无法直观得知其为一个“可能产生异常”的方法，
 * 即使能够得知其可能抛出的异常，上层调用方也可选择不处理该异常，则异常仍会根据调用栈向上层传递，以这个角度来说，
 * 该异常的传递其实是游离在方法的返回值声明之外的单独通道，若方法声明为{@code R<T, XXException> method(...)},
 * 则调用方可立即清楚该方法为“可能产生异常”的方法，且调用方在获取结果时，必要对该结果对象进行“解包”操作，
 * 以显式以某种方式对结果对象进行处理，这样可以避免异常从调用栈底一直传递到上层。<br>
 * 该类型目前的实现可能不是一种最好的实现方式，因为该类型目前无法形容“无返回且可能抛出异常”的方法，
 * 即该一定不为{@code R<Void, ?>}（类似{@code Optional<Void>}），以这个角度来说，
 * 该类型目前的实现是残缺的，但纷乱信息中无法总结出目前的残缺现状是因为java无法表达形容Rust中的{@code Result<(), Err>}，
 * 还是因为我菜，但先将信息记录在此，后续若有更好思路等，应改善优化该。
 * （又或许，在一种语言范式中强行以另一种语言的范式编程，本身就是一种错误？）<br>
 * 若该类型的使用环境为jdk21+，则可进行switch表达式模式匹配以解包该类型<br>
 * 关于相关问题以及相关类的各种讨论及参考，暂记于此：<br>
 * <a href="https://www.reddit.com/r/java/comments/1935m0r/is_there_a_result_type_in_java/">is_there_a_result_type_in_java</a><br>
 * <a href="https://github.com/gorandalum/fluent-result">fluent-result</a><br>
 * <a href="https://mail.openjdk.org/pipermail/amber-spec-experts/2023-December/003959.html">Effect cases in switch</a><br>
 * <a href="https://blog.jooq.org/javas-checked-exceptions-are-just-weird-union-types/">javas-checked-exceptions-are-just-weird-union-types</a><br>
 * <a href="https://old.reddit.com/r/java/comments/18hglp5/effect_cases_in_switch_brian_goetz/">effect_cases_in_switch_brian_goetz</a><br>
 * <a href="https://www.reddit.com/r/java/comments/qk6skd/smuggling_checked_exceptions_with_sealed/">smuggling_checked_exceptions_with_sealed</a><br>
 * <a href="https://mccue.dev/pages/11-1-21-smuggling-checked-exceptions">11-1-21-smuggling-checked-exceptions</a><br>
 * <a href="https://github.com/xyzsd/dichotomy">dichotomy</a><br>
 * <a href="https://www.baeldung.com/vavr-try">vavr-try</a>
 * @see R.Ok
 * @see R.Err
 * @since 2024/6/18 v0.0.3
 */
public sealed interface R<T, E> extends Serializable
        permits R.Ok, R.Err {

    /**
     * 明确创建"正常结果"<br>
     * 给定非空的“正常结果值”，明确的构造“正常结果”，返回{@link R.Ok}
     *
     * @param ok    非空的正常结果值
     * @param <Ok>  正常结果类型
     * @param <Err> 错误结果类型
     * @return “正常结果”
     * @throws NullPointerException 当给定的“正常结果值”为空时
     */
    static <Ok, Err> R.Ok<Ok, Err> ofOk(Ok ok) throws NullPointerException {
        return new R.Ok<>(ok);
    }


    /**
     * 明确创建"错误结果"<br>
     * 给定非空的“错误结果值”，明确的构造“错误结果”，返回{@link R.Err}
     *
     * @param err   非空的错误结果值
     * @param <Ok>  正常结果类型
     * @param <Err> 错误结果类型
     * @return “错误结果”
     * @throws NullPointerException 当给定的“错误结果值”为空时
     */
    static <Ok, Err> R.Err<Ok, Err> ofErr(Err err) throws NullPointerException {
        return new R.Err<>(err);
    }

    /**
     * 尝试创建"正常结果"<br>
     * 给定{@code nullable}的“正常结果值”，若其为{@code null}，
     * 将创建“错误结果”{@link R.Err}载荷{@link NullPointerException}，
     * 否则创建“正常结果"{@link R.Ok}
     *
     * @param ok   正常结果值(nullable)
     * @param <Ok> 正常结果值类型
     * @return 正常结果 | 错误结果
     */
    static <Ok> R<Ok, NullPointerException> ofNullable(Ok ok) {
        try {
            return ofOk(ok);
        } catch (NullPointerException e) {
            return ofErr(e);
        }
    }

    /**
     * 创建可能为“正常”也可能为“错误”的结果<br>
     * 给定可能为空的“正常结果值”，若“正常结果值”不为空，则返回{@link R.Ok},
     * 若“正常结果值”为空，则尝试使用给定的“错误结果值”，返回{@link R.Err}，
     * 若“错误结果值”为空，则抛出异常{@link NullPointerException}
     *
     * @param nullable 正常结果值(nullable)
     * @param err      错误结果值
     * @param <Ok>     正常结果类型
     * @param <Err>    错误结果类型
     * @return 正常结果 | 错误结果
     * @throws NullPointerException 当给定的“错误结果值”为空时
     */
    static <Ok, Err> R<Ok, Err> ofNullable(Ok nullable, Err err) {
        Objects.requireNonNull(err, "given 'err' must not be null");
        try {
            return ofOk(nullable);
        } catch (NullPointerException ignore) {
            return ofErr(err);
        }
    }

    /**
     * 创建可能为“正常”也可能为“错误”的结果<br>
     * 给定可能为空的“正常结果值”，若“正常结果值”不为空，则返回{@link R.Ok},
     * 若“正常结果值”为空，则尝试执行给定的“错误结果值提供函数”，返回{@link R.Err}，
     * 若“错误结果值提供函数”执行时后的“错误结果值”为空，则抛出{@link NullPointerException}
     * <pre>
     *     {@code
     *     R<String, Exception> r = R.ofNullable(strOrNull, NullPointException::new)
     *     }
     * </pre>
     *
     * @param nullable 正常结果(nullable)
     * @param fnGetErr 错误结果值提供函数
     * @param <Ok>     正常结果类型
     * @param <Err>    错误结果类型
     * @return 正常结果 | 错误结果
     * @throws NullPointerException 当给定的“错误结果值提供函数”为空或其执行后返回的“错误结果值”为空时
     */
    static <Ok, Err> R<Ok, Err> ofNullable(Ok nullable,
                                           Supplier<? extends Err> fnGetErr) {
        Objects.requireNonNull(fnGetErr, "given 'fnGetErr' must not be null");
        try {
            return ofOk(nullable);
        } catch (NullPointerException ignore) {
            return ofErr(fnGetErr.get());
        }
    }

    /**
     * 创建可能为“正常”也可能为“错误”的结果<br>
     * 给定“正常结果值提供函数”，若“正常结果值提供函数”不为空且其返回的“正常结果值”不为空，
     * 则返回{@link R.Ok},若“正常结果值”为空，或执行“正常结果值提供函数”过程中发生异常，
     * 则尝试使用给定的“错误结果值”，返回{@link R.Err}，
     * 若“错误结果值”为空，则抛出异常{@link NullPointerException}
     * <pre>
     *     {@code
     *     Optional<Integer> intOpt = Optional.ofNullable(nullableInt);
     *     R<Integer, String> r = R.ofSupplier(intOpt::get, "optional is empty");
     *     }
     * </pre>
     *
     * @param fnGetOk 正常结果值提供函数
     * @param err     错误结果值
     * @param <Ok>    正常结果类型
     * @param <Err>   错误结果类型
     * @return 正常结果 | 错误结果
     * @throws NullPointerException 当给定的“错误结果值”为空时
     */
    static <Ok, Err> R<Ok, Err> ofSupplier(Supplier<? extends Ok> fnGetOk, Err err) {
        Objects.requireNonNull(err, "given 'err' must not be null");
        try {
            return ofOk(fnGetOk.get());
        } catch (Exception fnGetOkIsNullOrExecErrOrFnReturnNull) {
            return ofErr(err);
        }
    }

    /**
     * 创建可能为“正常”也可能为“错误”的结果<br>
     * 给定“正常结果值提供函数”，若“正常结果值提供函数”不为空且其返回的“正常结果值”不为空，
     * 则返回{@link R.Ok},若“正常结果值”为空，或执行“正常结果值提供函数”过程中发生异常，
     * 则尝试执行给定的“错误结果值提供函数”，返回{@link R.Err}，
     * 若“错误结果值提供函数”为空或其执行后返回的“错误结果值”为空，
     * 则抛出异常{@link NullPointerException}
     * <pre>
     *     {@code
     *     Optional<Integer> intOpt = Optional.ofNullable(nullableInt);
     *     R<Integer, Exception> r = R.ofSupplier(intOpt::get, NullPointException::new);
     *     }
     * </pre>
     *
     * @param fnGetOk  正常结果值提供函数
     * @param fnGetErr 错误结果值提供函数
     * @param <Ok>     正常结果类型
     * @param <Err>    错误结果类型
     * @return 正常结果 | 错误结果
     * @throws NullPointerException 当给定的“错误结果值提供函数”为空或其执行后返回的“错误结果值”为空时
     */
    static <Ok, Err> R<Ok, Err> ofSupplier(Supplier<? extends Ok> fnGetOk,
                                           Supplier<? extends Err> fnGetErr) {
        Objects.requireNonNull(fnGetErr, "given 'fnGetErr' must not be null");
        try {
            return ofOk(fnGetOk.get());
        } catch (Exception fnGetOkIsNullOrExecErrOrFnReturnNull) {
            return ofErr(fnGetErr.get());
        }
    }

    /**
     * 执行给定的函数，返回可能为“正常”也可能为“错误”的结果<br>
     * 当函数执行过程中发生异常时，该方法将返回“错误结果”，否则返回“正常结果”（即函数返回值）<br>
     * 除了{@link Callable}，该函数也可为{@link io.github.baifangkual.bfk.j.mod.core.function.FnGet}类型
     *
     * @param fn   正常结果值提供函数
     * @param <Ok> 正常结果类型
     * @return 正常结果 | 错误结果
     * @throws NullPointerException 当给定的“正常结果值提供函数”为空时
     * @see #ofFnSupplier(Supplier)
     */
    static <Ok> R<Ok, Exception> ofFnCallable(Callable<? extends Ok> fn) {
        Objects.requireNonNull(fn, "given 'fn' must not be null");
        try {
            return ofOk(fn.call());
        } catch (Exception execFnCallErrOrFnReturnNull) {
            return ofErr(execFnCallErrOrFnReturnNull);
        }
    }

    /**
     * 执行给定的函数，返回可能为“正常”也可能为“错误”的结果<br>
     * 当函数执行过程中发生异常时，该方法将返回“错误结果”，否则返回“正常结果”（即函数返回值）<br>
     *
     * @param fn   正常结果值提供函数
     * @param <Ok> 正常结果类型
     * @return 正常结果 | 错误结果
     * @throws NullPointerException 当给定的“正常结果值提供函数”为空时
     * @see #ofFnCallable(Callable)
     */
    static <Ok> R<Ok, RuntimeException> ofFnSupplier(Supplier<? extends Ok> fn) {
        Objects.requireNonNull(fn, "given 'fn' must not be null");
        try {
            return ofOk(fn.get());
        } catch (RuntimeException execFnErrOrReturnNull) {
            return ofErr(execFnErrOrReturnNull);
        }
    }

    /**
     * 返回是否为"正常结果"
     *
     * @return true表示为正常结果，false表示为错误结果
     */
    boolean isOk();

    /**
     * 返回是否为"错误结果"
     *
     * @return true表示为错误结果，false表示为正常结果
     */
    boolean isErr();

    /**
     * 获取正常结果<br>
     * 当容器对象为"错误结果"对象时，抛出异常
     *
     * @return 正常结果
     * @throws ResultUnwrapException 当容器对象为错误结果时
     */
    T ok() throws ResultUnwrapException;

    /**
     * 尝试获取正常结果<br>
     * 当容器对象为"错误结果"对象时，该方法返回{@link Optional#empty()}
     *
     * @return 正常结果Optional
     * @see #toOptional()
     */
    Optional<T> tryOk();

    /**
     * 获取错误结果<br>
     * 当容器对象为"正常结果"对象时，抛出异常
     *
     * @return 错误结果
     * @throws ResultUnwrapException 当容器对象为正常结果时
     */
    E err() throws ResultUnwrapException;

    /**
     * 尝试获取错误结果<br>
     * 当容器对象为"正常结果"对象时，该方法返回{@link Optional#empty()}
     *
     * @return 错误结果Optional
     */
    Optional<E> tryErr();

    /**
     * 返回{@link Optional}对象，尝试获取正常结果<br>
     * 当容器对象为"错误结果"对象时，该方法返回{@link Optional#empty()}
     *
     * @return 正常结果Optional
     * @see #tryOk()
     */
    default Optional<T> toOptional() {
        return tryOk();
    }

    /**
     * 解包该实体以尝试获取{@link T}<br>
     * 当该实体为{@link R.Ok}时能够成功解包并返回{@link T}，
     * 否则将立即抛出{@link ResultUnwrapException}<br>
     * 若该实体为{@link R.Err}且{@link E}为{@link Throwable}时，
     * 将使其作为抛出的{@link ResultUnwrapException}的{@code cause}，
     * 否则，抛出的{@link ResultUnwrapException}没有{@code cause}
     *
     * @return 正常结果值（或直接抛出异常）
     * @throws ResultUnwrapException 当该实体为“错误结果”时
     * @see #unwrap(Supplier)
     * @see #unwrapOr(Object)
     * @see #unwrapOrGet(Supplier)
     */
    T unwrap() throws ResultUnwrapException;

    /**
     * 解包该实体以尝试获取{@link T}，若失败则抛出指定异常<br>
     *
     * @param <X>                   要抛出的异常类型
     * @param fnGetThrowableIfNotOk 异常提供函数
     * @return 正常结果值（或直接抛出指定异常）
     * @throws X                    当没有“正常结果值”时
     * @throws NullPointerException 当没有“正常结果值”且异常提供函数为空时
     * @apiNote 异常提供函数可以方法引用形式引用异常的无参构造，形如：
     * {@code IllegalStateException::new}
     * @see #unwrap()
     * @since 0.0.5
     */
    <X extends Throwable> T unwrap(Supplier<? extends X> fnGetThrowableIfNotOk) throws X;


    /**
     * 解包该实体以尝试获取{@link T}，若失败则返回给定的默认值<br>
     * 给定的默认值可以为{@code null}
     *
     * @param or 默认值
     * @return 正常结果值 | 默认值
     * @see #unwrap()
     * @see #unwrapOrGet(Supplier)
     */
    T unwrapOr(T or);

    /**
     * 解包该实体以尝试获取{@link T}，若失败则运行给定函数返回函数结果<br>
     * 给定函数的返回值可以为{@code null}
     *
     * @param fnGet 默认值提供函数
     * @return 正常结果值 | 默认值
     * @throws NullPointerException 当未能成功解包且给定的默认值提供函数为空时
     * @see #unwrap()
     * @see #unwrapOr(Object)
     */
    T unwrapOrGet(Supplier<? extends T> fnGet);


    /**
     * 如果“正常结果值”存在，则执行给定消费者函数
     *
     * @param fnAccOk 消费者函数
     * @throws NullPointerException 当“正常结果值”存在且消费者函数为空时
     * @since 0.0.5
     */
    void ifOk(Consumer<? super T> fnAccOk);

    /**
     * 如果“错误结果值”存在，则执行给定消费者函数
     *
     * @param fnAccErr 消费者函数
     * @throws NullPointerException 当“错误结果值”存在且消费者函数为空时
     * @since 0.0.5
     */
    void ifErr(Consumer<? super E> fnAccErr);

    /**
     * 当“正常结果值”存在，则返回只有一个元素的{@link Stream}，
     * 否则，返回空流{@link Stream#empty()}
     *
     * @return 包含一个“正常结果值”的流 | 空流{@link Stream#empty()}
     * @apiNote 该API适用于下场景（该方法会将{@link E}丢弃，使用方需考量适用与否）：
     * <pre>{@code
     *     Stream<R<T, E>> os = ...
     *     Stream<T> s = os.flatMap(R::stream)
     * }</pre>
     * @since 0.0.5
     */
    Stream<T> stream();

    /**
     * 根据给定函数，尝试转换“正常结果值”并返回新的{@link R}对象<br>
     * 当前实体为“正常结果”时，给定的函数将被执行以进行“正常结果值”的转换，
     * 当前实体为“错误结果”时，给定的函数将不会被执行
     *
     * @param fn  正常结果值转换函数
     * @param <U> 新的“正常结果值”类型
     * @return 新的结果对象
     * @throws NullPointerException 当给定的函数为空时
     * @throws NullPointerException 当给定函数执行后返回值为空时
     */
    default <U> R<U, E> map(Function<? super T, ? extends U> fn) {
        return isOk() ? ofOk(Objects.requireNonNull(fn, "fn is null").apply(ok())) : ofErr(err());
    }

    /**
     * 根据给定函数，尝试转换“正常结果值”到“{@link R}对象”，并返回该对象<br>
     * 当前实体为“正常结果”时，给定的函数将被执行以进行“正常结果值”到“结果对象”的转换，
     * 当前实体为“错误结果”时，给定的函数将不会被执行
     *
     * @param fn  正常结果值到{@link R}的转换函数
     * @param <U> 新的“正常结果值”类型
     * @return 新的结果对象
     * @throws NullPointerException 当给定的函数为空或给定函数执行后返回值为空时
     */
    default <U> R<U, E> flatmap(Function<? super T, ? extends R<? extends U, ? extends E>> fn) {
        if (isOk()) {
            Objects.requireNonNull(fn, "fn is null");
            @SuppressWarnings("unchecked")
            R<U, E> apply = (R<U, E>) fn.apply(ok());
            return Objects.requireNonNull(apply, "flatmap apply return is null");
        }
        return ofErr(err());
    }

    /**
     * 根据给定函数，尝试转换“错误结果值”并返回新的{@link R}对象<br>
     * 当前实体为“错误结果”时，给定的函数将被执行以进行“错误结果值”的转换，
     * 当前实体为“正常结果”时，给定的函数将不会被执行
     *
     * @param fn  错误结果值转换函数
     * @param <P> 新的“错误结果值”类型
     * @return 新的结果对象
     * @throws NullPointerException 当给定的函数为空时
     * @throws NullPointerException 当给定函数执行后返回值为空时
     */
    default <P> R<T, P> mapErr(Function<? super E, ? extends P> fn) {
        return isErr() ? ofErr(Objects.requireNonNull(fn, "fn is null").apply(err())) : ofOk(ok());
    }

    /**
     * 根据给定函数，尝试转换“错误结果值”到“{@link R}对象”，并返回该对象<br>
     * 当前实体为“错误结果”时，给定的函数将被执行以进行“错误结果值”到“结果对象”的转换，
     * 当前实体为“正常结果”时，给定的函数将不会被执行
     *
     * @param fn  错误结果值到{@link R}的转换函数
     * @param <P> 新的“错误结果值”类型
     * @return 新的结果对象
     * @throws NullPointerException 当给定的函数为空或给定函数执行后返回值为空时
     */
    default <P> R<T, P> flatmapErr(Function<? super E, ? extends R<? extends T, ? extends P>> fn) {
        if (isErr()) {
            Objects.requireNonNull(fn, "fn is null");
            @SuppressWarnings("unchecked")
            R<T, P> apply = (R<T, P>) fn.apply(err());
            return Objects.requireNonNull(apply, "flatmap apply return is null");
        }
        return ofOk(ok());
    }


    @Override
    boolean equals(Object o);

    @Override
    int hashCode();

    @Override
    String toString();


    /**
     * 正确结果
     *
     * @param <Ok>  正确结果值类型
     * @param <Err> 错误结果值类型
     * @see R
     */
    record Ok<Ok, Err>(Ok ok) implements R<Ok, Err>, Serializable {
        @Serial
        private static final long serialVersionUID = 33L;

        public Ok {
            Objects.requireNonNull(ok, "'ok' is null");
        }


        @Override
        public String toString() {
            return "Ok(" + ok + ')';
        }


        @Override
        public boolean isOk() {
            return true;
        }

        @Override
        public boolean isErr() {
            return false;
        }

        @Override
        public Optional<Ok> tryOk() {
            return Optional.of(ok);
        }

        @Override
        public Err err() throws ResultUnwrapException {
            throw new ResultUnwrapException("not found 'err' value");
        }

        @Override
        public Optional<Err> tryErr() {
            return Optional.empty();
        }

        @Override
        public Ok unwrap() throws ResultUnwrapException {
            return ok;
        }

        @Override
        public Ok unwrapOr(Ok or) {
            return ok;
        }

        @Override
        public Ok unwrapOrGet(Supplier<? extends Ok> fnGet) {
            return ok;
        }

        @Override
        public <X extends Throwable> Ok unwrap(Supplier<? extends X> fnGetThrowableIfNotOk) throws X {
            return ok;
        }

        @Override
        public void ifOk(Consumer<? super Ok> fnAccOk) {
            fnAccOk.accept(ok());
        }

        @Override
        public void ifErr(Consumer<? super Err> fnAccErr) {
            // pass... do nothing because I'm an 'Ok'
        }

        @Override
        public Stream<Ok> stream() {
            return Stream.of(ok);
        }
    }

    /**
     * 错误结果
     *
     * @param <Ok>  正确结果值类型
     * @param <Err> 错误结果值类型
     * @see R
     */
    record Err<Ok, Err>(Err err) implements R<Ok, Err>, Serializable {
        @Serial
        private static final long serialVersionUID = 33L;

        public Err {
            Objects.requireNonNull(err, "'err' is null");
        }

        @Override
        public String toString() {
            return "Err(" + err + ')';
        }

        @Override
        public boolean isOk() {
            return false;
        }

        @Override
        public boolean isErr() {
            return true;
        }

        @Override
        public Ok ok() throws ResultUnwrapException {
            throw new ResultUnwrapException("not found 'ok' value");
        }

        @Override
        public Optional<Ok> tryOk() {
            return Optional.empty();
        }

        @Override
        public Optional<Err> tryErr() {
            return Optional.of(err);
        }

        @Override
        public Ok unwrap() throws ResultUnwrapException {
            // 当err类型为异常(可抛出)时
            if (err instanceof Throwable e) {
                throw new ResultUnwrapException(e);
            } else {
                // 当err类型不为异常(可抛出)时, 包装其
                throw new ResultUnwrapException("not an 'Ok'");
            }
        }

        @Override
        public Ok unwrapOr(Ok or) {
            return or;
        }

        @Override
        public Ok unwrapOrGet(Supplier<? extends Ok> fnGet) {
            return Objects.requireNonNull(fnGet, "fnGet is null").get();
        }

        @Override
        public <X extends Throwable> Ok unwrap(Supplier<? extends X> fnGetThrowableIfNotOk) throws X {
            throw fnGetThrowableIfNotOk.get();
        }

        @Override
        public void ifOk(Consumer<? super Ok> fnAccOk) {
            // pass... do nothing because I'm an 'Err'
        }

        @Override
        public void ifErr(Consumer<? super Err> fnAccErr) {
            fnAccErr.accept(err());
        }

        @Override
        public Stream<Ok> stream() {
            return Stream.empty();
        }
    }


}


