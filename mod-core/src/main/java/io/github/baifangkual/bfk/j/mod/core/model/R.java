package io.github.baifangkual.bfk.j.mod.core.model;

import io.github.baifangkual.bfk.j.mod.core.exception.PanicException;
import io.github.baifangkual.bfk.j.mod.core.exception.ResultUnwrapException;
import io.github.baifangkual.bfk.j.mod.core.exception.ResultWrapException;
import io.github.baifangkual.bfk.j.mod.core.fmt.STF;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * <b>结果（Result）</b><br>
 * 一个不可变容器 线程安全，表达可能操作失败的逻辑的结果，该类型实例要么携带“正常结果”({@link Ok})，要么携带“错误结果”({@link Err})，
 * 该类内缺省主语的方法签名主语默认形容的是{@link Ok}<br>
 * 若操作成功，则{@link #isOk()}为true，{@link #isErr()}为false,
 * 调用{@link #ok()}将返回“正常结果”，而调用{@link #err()}则抛出异常，
 * 若操作失败，则{@link #isOk()}为false，{@link #isErr()}为true,
 * 调用{@link #err()}将返回“错误结果”，而调用{@link #ok()}则抛出异常<br>
 * 类似{@link Optional}约束的那样，该类型的引用应当永远不为null<br>
 *
 * @author baifangkual
 * @apiNote {@link Err}并未约束一定为{@link Exception}类型的子级，换句话说，{@link R}携带的“错误结果”并不一定是java异常，
 * 使用方应确保自身代码中所有“错误结果”为{@link Exception}类型及其子级的{@link R}对象都最终被解包处理了，否则可能造成部分java异常被静默丢弃。
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
 * 因为jdk17的switch并不能支持模式匹配，所以以该方式实现R类型<br>
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
 * @since 2024/6/18 v0.0.3
 */
public class R<Ok, Err> implements Serializable {

    @Serial
    private static final long serialVersionUID = 114514L;
    /**
     * 存储“错误结果”
     */
    final Err err;
    /**
     * 存储“正常结果”
     */
    final Ok ok;


    /**
     * 私有构造，不允许外界直接调用构造函数，携带的“正常”和“错误”结果对象均可能为空，但不会同时为空或同时不为空
     *
     * @param isOk 明确表达容器对象状态是否“正常”
     * @param ok   携带的“正常结果”对象，可能为空
     * @param err  携带的“错误结果”对象，可能为空
     * @throws ResultWrapException 当给定的参数无法构造实体时
     */
    private R(boolean isOk, Ok ok, Err err) {
        final boolean haveSome = ok != null;
        final boolean haveErr = err != null;
        /* runtime check state */
        if (isOk && haveErr) {
            throw new ResultWrapException("R type Ok, but R.err is no-null, R.err: " + err);
        } else if (!isOk && !haveErr) {
            throw new ResultWrapException("R type Err, but R.err is null");
        } else if (isOk && !haveSome) {
            throw new ResultWrapException("R type Ok, but R.ok is null");
        } else if (!isOk && haveSome) {
            throw new ResultWrapException("R type Err, but R.ok is no-null, R.ok: " + ok);
        }
        this.err = err;
        this.ok = ok;
    }

    /**
     * 私有构造，不允许外界调用，根据给定的参数构造“正常”或“错误”结果，携带的“正常”和“错误”结果对象均可能为空，但不会同时为空或同时不为空
     *
     * @param ok  携带的“正常结果”对象，可能为空
     * @param err 携带的“错误结果”对象，可能为空
     * @throws ResultWrapException 当给定的参数无法构造实体时
     */
    private R(Ok ok, Err err) {
        this(ok != null, ok, err);
    }

    /**
     * 明确创建"正常结果"<br>
     * 给定非空的对象，明确的构造“正常结果”
     *
     * @param ok   非空的正常结果
     * @param <Ok> 正常结果类型
     * @param <E>  错误结果类型
     * @return “正常结果”
     * @throws ResultWrapException 当给定的参数为空时
     */
    public static <Ok, E> R<Ok, E> ofOk(Ok ok) {
        return new R<>(true, ok, null);
    }

    /**
     * 明确创建"错误结果"<br>
     * 给定非空对象，明确的构造“错误结果”
     *
     * @param err  非空的错误结果
     * @param <Ok> 正常结果类型
     * @param <E>  错误结果类型
     * @return “错误结果”
     * @throws ResultWrapException 当给定的参数为空时
     */
    public static <Ok, E> R<Ok, E> ofErr(E err) {
        return new R<>(false, null, err);
    }

    /**
     * 创建可能“正常”也可能“错误”的结果<br>
     * 给定非空的“正常结果”或非空的“错误结果”，给定的参数不得同时为null或同时不为null
     *
     * @param ok    正常结果 or null
     * @param orErr 错误结果 or null
     * @param <Ok>  正常结果类型
     * @param <E>   错误结果类型
     * @return 正常 or 错误结果（取决于给定的参数）
     * @throws ResultWrapException 当给定的参数同为null或同不为null时
     */
    public static <Ok, E> R<Ok, E> of(Ok ok, E orErr) {
        return new R<>(ok, orErr);
    }

    /**
     * 创建可能“正常”也可能“错误”的结果<br>
     * 给定非空的“正常结果”或“错误结果提供函数”，当给定的“正常结果”为空时，
     * 将执行给定的“错误结果提供函数”，以获取“错误结果”并构造该对象
     *
     * @param ok         正常结果 or null
     * @param orFnGetErr 错误结果提供函数
     * @param <Ok>       正常结果类型
     * @param <E>        错误结果类型
     * @return 正常 or 错误结果（取决于给定的参数和函数执行结果）
     * @throws ResultWrapException 当给定的“错误结果提供函数”为null时
     */
    public static <Ok, E> R<Ok, E> of(Ok ok, Supplier<? extends E> orFnGetErr) {
        if (ok == null) {
            if (orFnGetErr == null) throw new ResultWrapException("orFnGetErr is null");
            return ofErr(orFnGetErr.get());
        }
        return ofOk(ok);
    }

    /**
     * 创建可能“正常”也可能“错误”的结果<br>
     * 给定非空的“正常结果提供函数”或“错误结果提供函数”，当给定的“正常结果提供函数”执行结果为null，
     * 将执行给定的“错误结果提供函数”，以获取“错误结果”并构造该对象
     *
     * @param fnGetOk    正常结果提供函数
     * @param orFnGetErr 错误结果提供函数
     * @param <Ok>       正常结果类型
     * @param <E>        错误结果类型
     * @return 正常 or 错误结果（取决于给定的函数执行结果）
     * @throws ResultWrapException 当给定的“结果提供函数”为null时
     */
    @SuppressWarnings("UnusedReturnValue")
    public static <Ok, E> R<Ok, E> of(Supplier<? extends Ok> fnGetOk,
                                      Supplier<? extends E> orFnGetErr) {
        if (fnGetOk == null) throw new ResultWrapException("fnGetOk is null");
        return of(fnGetOk.get(), orFnGetErr);
    }

    /**
     * 执行给定的函数，返回可能“正常”也可能“错误”的结果<br>
     * 当函数执行过程中发生异常时，该方法将返回“错误结果”，否则返回“正常结果”（即函数返回值）<br>
     * 除了{@link Callable}，该函数也可为{@link io.github.baifangkual.bfk.j.mod.core.function.FnGet}类型
     *
     * @param fn   函数
     * @param <Ok> 正常结果类型
     * @return 正常 or 错误结果（取决于给定的函数执行结果）
     * @throws ResultWrapException 当给定的“正常结果提供函数”为null时
     * @see #ofFnSup(Supplier)
     */
    public static <Ok> R<Ok, Exception> ofFnCall(Callable<? extends Ok> fn) {
        try {
            if (fn == null) throw new ResultWrapException("fn is null");
            return R.ofOk(fn.call());
        } catch (Exception e) {
            return R.ofErr(e);
        }
    }

    /**
     * 执行给定的函数，返回可能“正常”也可能“错误”的结果<br>
     * 当函数执行过程中发生异常时，该方法将返回“错误结果”，否则返回“正常结果”（即函数返回值）<br>
     *
     * @param fn   函数
     * @param <Ok> 正常结果类型
     * @return 正常 or 错误结果（取决于给定的函数执行结果）
     * @throws ResultWrapException 当给定的“正常结果提供函数”为null时
     * @see #ofFnCall(Callable)
     */
    public static <Ok> R<Ok, RuntimeException> ofFnSup(Supplier<? extends Ok> fn) {
        try {
            if (fn == null) throw new ResultWrapException("fn is null");
            return R.ofOk(fn.get());
        } catch (RuntimeException e) {
            return R.ofErr(e);
        }
    }

    /**
     * 返回是否为正常结果的布尔值
     *
     * @return true表示为正常结果，false表示为错误结果
     */
    public boolean isOk() {
        return this.ok != null;
    }

    /**
     * 返回是否为错误结果的布尔值
     *
     * @return true表示为错误结果，false表示为正常结果
     */
    public boolean isErr() {
        return this.err != null;
    }

    /**
     * 获取正常结果<br>
     * 当容器对象为错误结果对象时，抛出异常
     *
     * @return 正常结果
     * @throws ResultUnwrapException 当容器对象为错误结果时
     */
    @SuppressWarnings("UnusedReturnValue")
    public Ok ok() {
        if (isErr())
            throw new ResultUnwrapException("R type Err, R.ok is null");
        return ok;
    }

    /**
     * 尝试获取正常结果<br>
     * 当容器对象为错误结果对象时，该方法返回{@link Optional#empty()}
     *
     * @return 正常结果Optional
     * @see #toOptional()
     */
    public Optional<Ok> tryOk() {
        return isOk() ? Optional.of(ok) : Optional.empty();
    }

    /**
     * 获取错误结果<br>
     * 当容器对象为正常结果对象时，抛出异常
     *
     * @return 错误结果
     * @throws ResultUnwrapException 当容器对象为正常结果时
     */
    public Err err() {
        if (isOk())
            throw new ResultUnwrapException("R type Ok, R.err is null");
        return err;
    }

    /**
     * 尝试获取错误结果<br>
     * 当容器对象为正常结果对象时，该方法返回{@link Optional#empty()}
     *
     * @return 错误结果Optional
     */
    public Optional<Err> tryErr() {
        return isErr() ? Optional.of(err) : Optional.empty();
    }

    /**
     * 返回{@link Optional}对象，尝试获取正常结果<br>
     * 当容器对象为错误结果对象时，该方法返回{@link Optional#empty()}
     *
     * @return 正常结果Optional
     * @see #tryOk()
     */
    public Optional<Ok> toOptional() {
        return tryOk();
    }

    /**
     * 直接解包该{@link R}容器对象，当结果为{@link Ok}时正常返回，否则将立即抛出运行时异常<br>
     * 当{@link Err}类型为java运行时异常时，直接抛出该运行时异常，当为java预检异常时，
     * 包装为{@link PanicException}并抛出，当不为java异常时，将抛出{@link ResultUnwrapException}
     *
     * @return 正常结果（或直接抛出异常）
     * @throws ResultUnwrapException  当该实体未载荷“正常结果”且载荷的“错误结果”不为java异常类时
     * @throws PanicException   当该实体未载荷“正常结果”且载荷的“错误结果”为java预检异常时
     * @throws RuntimeException 当该实体未载荷“正常结果”且载荷的“错误结果”为java相应的运行时异常时
     * @see #unwrapOr(Object)
     * @see #unwrapOrGet(Supplier)
     */
    public Ok unwrap() {
        if (isOk()) {
            return ok;
        } else {
            // 当err类型为异常(可抛出)时，做处理
            if (err instanceof Throwable e) {
                if (e instanceof RuntimeException rte) {
                    throw rte;
                } else {
                    throw PanicException.wrap(e);
                }
            } else {
                // 当err类型不为异常(可抛出)时, 包装其
                throw new ResultUnwrapException("R type Err, R.err(" + err + ")");
            }
        }
    }

    /**
     * 直接解包该{@link R}容器对象，当结果为{@link Ok}时正常返回，否则则返回给定的默认值<br>
     * 给定的默认值可以为null
     *
     * @param orOk 默认值
     * @return 正常结果
     * @see #unwrap()
     * @see #unwrapOrGet(Supplier)
     */
    public Ok unwrapOr(Ok orOk) {
        return isOk() ? ok : orOk;
    }

    /**
     * 直接解包该{@link R}容器对象，当结果为{@link Ok}时正常返回，否则则运行给定函数返回函数运行结果<br>
     * 给定函数的返回值可以为null
     *
     * @param sup 默认值提供函数
     * @return 正常结果
     * @see #unwrap()
     * @see #unwrapOr(Object)
     */
    public Ok unwrapOrGet(Supplier<? extends Ok> sup) {
        return isOk() ? ok : Objects.requireNonNull(sup.get(), "supplier fn is null");
    }

    /**
     * 根据给定函数，转换“正常结果”并生成新的{@link R}对象<br>
     * 当当前结果为“正常结果”时，给定的函数将被执行以进行“正常结果”的转换，
     * 当当前结果为“错误结果”时，给定的函数将不会被执行
     *
     * @param fn  转换函数
     * @param <O> 新的“正常结果”类型
     * @return 新的结果对象
     * @throws NullPointerException 当给定的函数为null时
     */
    public <O> R<O, Err> map(Function<? super Ok, ? extends O> fn) {
        return isOk() ? R.ofOk(Objects.requireNonNull(fn, "map fn is null").apply(ok)) : R.ofErr(err);
    }

    /**
     * 根据给定函数，转换“正常结果”到“结果对象”<br>
     * 当当前结果为“正常结果”时，给定的函数将被执行以进行“正常结果”到“结果对象”的转换，
     * 当当前结果为“错误结果”时，给定的函数将不会被执行
     *
     * @param fn  转换函数
     * @param <O> 新的“正常结果”类型
     * @return 新的结果对象
     * @throws NullPointerException 当给定的函数为null时
     */
    public <O> R<O, Err> flatmap(Function<? super Ok, ? extends R<? extends O, ? extends Err>> fn) {
        if (isOk()) {
            Objects.requireNonNull(fn, "flatmap fn is null");
            @SuppressWarnings("unchecked")
            R<O, Err> apply = (R<O, Err>) fn.apply(ok);
            return apply;
        }
        return R.ofErr(err);
    }

    /**
     * 根据给定函数，转换“错误结果”并生成新的{@link R}对象<br>
     * 当当前结果为“错误结果”时，给定的函数将被执行以进行“错误结果”的转换，
     * 当当前结果为“正常结果”时，给定的函数将不会被执行
     *
     * @param fn  转换函数
     * @param <E> 新的“错误结果”类型
     * @return 新的结果对象
     * @throws NullPointerException 当给定的函数为null时
     */
    public <E> R<Ok, E> mapErr(Function<? super Err, ? extends E> fn) {
        return isErr() ? R.ofErr(Objects.requireNonNull(fn, "map fn is null").apply(err)) : R.ofOk(ok);
    }

    /**
     * 根据给定函数，转换“错误结果”到“结果对象”<br>
     * 当当前结果为“错误结果”时，给定的函数将被执行以进行“错误结果”到“结果对象”的转换，
     * 当当前结果为“正常结果”时，给定的函数将不会被执行
     *
     * @param fn  转换函数
     * @param <E> 新的“错误结果”类型
     * @return 新的结果对象
     * @throws NullPointerException 当给定的函数为null时
     */
    public <E> R<Ok, E> flatmapErr(Function<? super Err, ? extends R<? extends Ok, ? extends E>> fn) {
        if (isErr()) {
            Objects.requireNonNull(fn, "flatmap fn is null");
            @SuppressWarnings("unchecked")
            R<Ok, E> apply = (R<Ok, E>) fn.apply(err);
            return apply;
        }
        return R.ofOk(ok);
    }


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof R<?, ?> r)) return false;
        return Objects.equals(err, r.err) && Objects.equals(ok, r.ok);
    }

    @Override
    public int hashCode() {
        return Objects.hash(err, ok);
    }

    /**
     * 返回{@link R}的ToString表现形式
     *
     * @return ToString表现形式
     */
    @Override
    public String toString() {
        return isOk() ? STF.f("R[Ok({})]", ok) : STF.f("R[Err({})]", err);
    }
}
