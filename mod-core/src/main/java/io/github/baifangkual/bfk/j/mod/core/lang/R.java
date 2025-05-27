package io.github.baifangkual.bfk.j.mod.core.lang;

import io.github.baifangkual.bfk.j.mod.core.func.Fn;
import io.github.baifangkual.bfk.j.mod.core.func.FnAcc;
import io.github.baifangkual.bfk.j.mod.core.func.FnRun;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * <b>结果（Result）</b><br>
 * 一个不可变容器，线程安全，表达可能操作失败的逻辑的结果<br>
 * <b>语义约定：</b>
 * <ul>
 *     <li>任何对 {@link R} 实例进行的操作导致 {@link R} 状态需发生变更时，都会返回新的 {@link R} 实例</li>
 *     <li>一个 {@link R} 实例仅应形容某一个操作的结果（一个操作是指能够被 {@code R.ofXXX()} 系列方法描述的操作，应可以理解为构建 {@link R} 实例这个过程本身），
 *     当 {@link R} 实例被构建后，其绑定的系列成员方法应仅是对其载荷的可能存在的“正常结果”或“错误结果”进行某操作的“语法糖”，
 *     即一系列成员方法提供的逻辑及操作都应该能够转换为：
 *     <pre>
 *         {@code
 *         if (r.isOk()) {// do something...}
 *         else {// do something...}
 *         }
 *     </pre>
 *     即当外界提供的这些操作本身“不合法”（例如提供函数，但函数引用为{@code null}）时，应在提供这些操作的上下文中抛出异常，
 *     即除了进行 {@code R.ofXXX(...)} 系列构建实例的操作外，其他操作并不能避免（也不应该避免）在上下文中产生异常
 *     </li>
 *     <li>若通过 {@link R} 的系列实例方法对 {@link R} 实例进行不符合 {@link R} 类型本身描述的语义的操作时（见上条）发生异常（并不代指R变为Err，而是代指在操作R的当前上下文发生异常）
 *     则说明外界给定的操作本身不合法（比如给定的函数为 {@code null}）</li>
 * </ul>
 * 该类型实例要么为“正常结果”({@link R.Ok})携带“正常结果值”({@link T})，
 * 要么为“错误结果”({@link R.Err})携带“错误结果值”({@link Exception})，
 * 该类内缺省主语的方法签名主语默认应形容 {@link T}<br>
 * 进行某操作后返回该类型实例，若操作成功，则{@link #isOk()}为true，{@link #isErr()}为false,
 * 调用{@link #ok()}将返回“正常结果”，而调用{@link #err()}则抛出异常，
 * 若操作失败，则{@link #isOk()}为false，{@link #isErr()}为true,
 * 调用{@link #err()}将返回“错误结果”，而调用{@link #ok()}则抛出异常<br>
 * 类似{@link Optional}约束的那样，对该类型的引用应当永远不为{@code null}<br>
 *
 * @param <T> 正确结果值类型
 * @author baifangkual
 * @apiNote 使用方应确保自身代码中所有 {@link R}对象 都最终被解包处理了，否则可能造成部分重要的 java异常 被静默丢弃。
 * @implNote 该类型的实现参考Rust语言中Result类型的行为和使用场景，抽象的讲，
 * 即想要在java语言中避免try-catch-throw这种单独的异常信息传输通道（对于一个执行可能产生异常的方法（尤其是运行时异常）），
 * 其方法声明形如 {@code T method(...)}，这种方法声明从外界使用者看来无法直观得知其为一个“可能产生异常”的方法，
 * 即使能够得知其可能抛出的异常，上层调用方也可选择不处理该异常，则异常仍会根据调用栈向上层传递，以这个角度来说，
 * 该异常的传递其实是游离在方法的返回值声明之外的单独通道，若方法声明为{@code R<T, XXException> method(...)},
 * 则调用方可立即清楚该方法为“可能产生异常”的方法，且调用方在获取结果时，必要对该结果对象进行“解包”操作，
 * 以显式以某种方式对结果对象进行处理，这样可以避免异常从调用栈一直传递到上层。<br>
 * （20250525更新说明：该Class已被重写，因为java中无法对泛型进行条件限定，遂当 R 为 {@code R<Integer, String>} 时，
 * 无法处理当调用map方法执行给定函数过程抛出异常而被 R 持有的情况：假设有步骤1、2，步骤1返回R.Ok，步骤2能够处理R中的“正常结果值”，
 * 则在原有设计中，使用map操作，若进行步骤2的时候发生异常，则操作中断，发生的异常还是按照java的原有异常逻辑向上层抛出了，
 * 这种情况虽然表示了R.Ok中的载荷无法通过步骤2变为另一类型实例，但其反馈的原因还没没法以R的形式返回（异常被抛出了），这与R的设计相背离，
 * 即这种情况下，R，是无用的。考虑过将map进行更改，其接收两个函数，第一个函数是map原有的语义，第二个函数表示当第一个函数执行过程发生异常时，
 * 如何将异常对象转为R.Err的载荷的泛型类型，这种变更似乎可行，但有两个问题，第一，这样设计在使用map时会很繁琐，第二，第一个函数执行过程中异常了，
 * 执行第二个函数，但第二个函数执行过程中也异常了，那如何通过 R 表达这种情况？显然无法表达这种情况，只有当 R 的第二泛型参数为 Exception是，
 * 能够无脑的将抛出的异常由 R.Err 载荷表达。第三种修改方式：将 {@code R<T, E>} 修改为 {@code R<T, E extends Exception>}，
 * 也不行，因为即使约定 E 的上界，假设 R 为 {@code R<String, IllegalStateException>}，但在map过程中抛出了 NullPointException，
 * 若map内不做处理，则该map过程中断，异常以其原本的通道向上层传递，若map内catch该异常，则将其赋值给 E 时因为不是IllegalStateException或其子级，
 * 会发生强转异常，只有当 E 为 Exception 时，才可安全操作。综上，决定移除 R 的第二泛型参数，R.Err 固定载荷的引用类型为 Exception）
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
public sealed interface R<T> extends Serializable
        permits R.Ok, R.Err {

    /**
     * 尝试创建"正常结果"<br>
     * 给定“正常结果值”，构造“正常结果”，返回 {@code R.Ok(ok)}，
     * 若”正常结果值“为 {@code null}，返回 {@code R.Err(NullPointerException)}
     *
     * @param ok  正常结果值
     * @param <T> 正常结果类型
     * @return {@code R.Ok(ok)} | {@code R.Err(NullPointerException)}
     */
    static <T> R<T> ofOk(T ok) {
        return ok == null ?
                new Err<>(new NullPointerException("'ok' is null"))
                : new Ok<>(ok);
    }

    /**
     * 尝试创建"错误结果"<br>
     * 给定“错误结果值”，构造“错误结果”，返回 {@code R.Err(err)},
     * 若”错误结果值“为 {@code null}，返回 {@code R.Err(NullPointerException)}
     *
     * @param err 错误结果值
     * @param <T> 正常结果类型
     * @return {@code R.Err(err)} | {@code R.Err(NullPointerException)}
     */
    static <T> R.Err<T> ofErr(Exception err) {
        return err == null ?
                new Err<>(new NullPointerException("'err' is null"))
                : new R.Err<>(err);
    }

    /**
     * 尝试创建"正常结果"<br>
     * 给定“正常结果值”，若“正常结果值”不为 {@code null}，则返回 {@code R.Ok(ok)}，
     * 若“正常结果值”为 {@code null}，则尝试使用给定的“错误结果值”，返回 {@code R.Err(err)}，
     * 若“错误结果值”为 {@code null}，则返回 {@code R.Err(NullPointerException)}
     *
     * @param ok  正常结果值
     * @param err 错误结果值
     * @param <T> 正常结果类型
     * @return {@code R.Ok(ok)} | {@code R.Err(err)} | {@code R.Err(NullPointerException)}
     */
    static <T> R<T> ofOk(T ok, Exception err) {
        return ok != null ? new Ok<>(ok)
                : err != null ?
                new Err<>(err) // err 不为空，返回之，否则返回E-NPE
                : new Err<>(new NullPointerException("'ok' and 'err' is null"));
    }

    /**
     * 尝试创建"正常结果"<br>
     * 给定“正常结果值”，若“正常结果值”不为 {@code null}，则返回 {@code R.Ok(ok)}，
     * 若“正常结果值”为 {@code null}，则尝试使用给定的“异常提供函数”，返回 {@code R.Err(err)}，
     * 若“异常提供函数”为 {@code null}，或其执行过程发生异常，或其执行后返回值为 {@code null}，
     * 则返回 {@code R.Err(NullPointerException)}
     * <pre>
     *     {@code
     *     R<String> r = R.ofOk(strOrNull, IllegalStateException::new)
     *     }
     * </pre>
     *
     * @param ok       正常结果
     * @param fnGetErr 异常提供函数
     * @param <T>      正常结果类型
     * @return {@code R.Ok(ok)} | {@code R.Err(err)} | {@code R.Err(...)}
     */
    static <T> R<T> ofOk(T ok,
                         Callable<? extends Exception> fnGetErr) {
        if (ok != null) return new Ok<>(ok);
        if (fnGetErr == null) //fnGetErr is null
            return new Err<>(new NullPointerException("'ok' and 'fnGetErr' is null"));
        try {
            Exception eOfGetNullable = fnGetErr.call(); // exception or return
            return new Err<>(Objects.requireNonNullElseGet(eOfGetNullable,
                    () -> new NullPointerException("'ok' is null and exec 'fnGetErr' return null")));
        } catch (Exception e) {
            // catch exception on fnGetErr exec.
            return new Err<>(new RuntimeException("'ok' is null and exec 'fnGetErr' throw exception", e));
        }
    }

    /**
     * 尝试从给定的 {@link Optional} 中获取值，若成功，返回 {@code R.Ok(T)}，
     * 否则，返回 {@code R.Err(NullPointException)}
     * <pre>
     *     {@code
     *     Optional<T> opt = ...;
     *     R<T> r = R.ofOptional(opt);
     *     if (opt.isEmpty()) {
     *         Assert.eq(r.err().getClass(), NullPointException.class);
     *     } else {
     *         Assert.eq(opt.get(), r.unwrap());
     *     }
     *     }
     * </pre>
     *
     * @param optional 可能存在的值
     * @param <T>      正常结果类型
     * @return {@code R.Ok(T)} | {@code R.Err(NullPointException)}
     * @apiNote 虽然给定该方法一个 {@code null} 并不会抛出异常，但 {@link Optional} 引用最好永不为 {@code null}
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static <T> R<T> ofOptional(Optional<? extends T> optional) {
        //noinspection OptionalAssignedToNull
        if (optional == null) return new Err<>(new NullPointerException("'optional' is null"));
        return ofFnCallable(optional::get);
    }

    /**
     * 尝试从给定的 {@link Future} 中获取值，创建“正常结果”，当其为异常完成，则获取其异常err返回 {@code R.Err(err}
     *
     * @param future future-当该为 {@code null} 时立即返回 {@code R.Err(NullPointException)}
     * @param <T>    正常结果类型
     * @return {@code R.Ok(T)} | {@code R.Err(err)} | {@code R.Err(NullPointException)}
     * @apiNote 如果给定的 {@link Future} 为 {@code Future<Void>}，则总是返回 {@code R.Err(NullPointException)}
     */
    static <T> R<T> ofFuture(Future<? extends T> future) {
        if (future == null) return new Err<>(new NullPointerException("'future' is null"));
        return ofFnCallable(() -> {
            try {
                final T t = future.get();
                if (t == null) throw new NullPointerException("'future' return null");
                return t;
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt(); // reset interrupt
                throw interruptedException;
            }
        });
    }

    /**
     * 返回 {@link CompletableFuture}<br>
     * 若当前为 {@code R.Ok}, 则返回 “成功完成的”，否则，返回 “异常完成的”
     *
     * @return {@code CompletedFuture} | {@code FailedFuture}
     */
    default CompletableFuture<T> future() {
        if (isOk()) {
            return CompletableFuture.completedFuture(ok());
        } else {
            return CompletableFuture.failedFuture(err());
        }
    }

    /**
     * 执行给定函数，若发生异常，发生的异常在返回的 {@code R.err()}
     *
     * @param fnRun 函数
     * @return {@code R.Ok(Nil)} | {@code R.Err(err)} | {@code R.Err(NullPointException)}
     */
    static R<Nil> ofFnRun(FnRun fnRun) {
        if (fnRun == null) return new Err<>(new NullPointerException("'fnRun' is null"));
        return fnRun.toSafe().get();
    }

    /**
     * 执行给定函数，若发生异常，发生的异常在返回的 {@code R.err()}
     *
     * @param runnable 函数
     * @return {@code R.Ok(Nil)} | {@code R.Err(err)} | {@code R.Err(NullPointException)}
     */
    static R<Nil> ofRunnable(Runnable runnable) {
        if (runnable == null) return new Err<>(new NullPointerException("'runnable' is null"));
        return ofFnRun(runnable::run);
    }

    /**
     * 表示“无返回值”
     *
     * @return {@code R.Ok(Nil)}
     */
    static R<Nil> ofNil() {
        return new Ok<>(Nil.self());
    }

    /**
     * 以入参执行给定函数，若发生异常，发生的异常在返回的 {@code R.err()}
     *
     * @param p     函数入参
     * @param fnAcc 函数
     * @return {@code R.Ok(Nil)} | {@code R.Err(err)} | {@code R.Err(NullPointException)}
     */
    static <P> R<Nil> ofFnAcc(FnAcc<? super P> fnAcc, P p) {
        if (fnAcc == null) return new Err<>(new NullPointerException("'fnAcc' is null"));
        return fnAcc.toSafe().apply(p);
    }

    /**
     * 尝试执行函数创建"正常结果"<br>
     * 给定“正常结果值提供函数”，若“正常结果值提供函数”不为 {@code null}，则执行其，
     * 若其执行后返回值不为 {@code null}，返回 {@code R.Ok(ok)}，
     * 若“正常结果值提供函数”为 {@code null} 或其执行过程发生异常，或其执行后返回 {@code null} ，
     * 则尝试使用给定的“错误结果值”，返回 {@code R.Err(err)}，
     * 若“错误结果值”为 {@code null}，则返回 {@code R.Err(NullPointerException)}
     * <pre>
     *     {@code
     *     Optional<Integer> intOpt = Optional.ofNullable(nullableInt);
     *     R<Integer> r = R.ofFnCallable(intOpt::get, new IllegalStateException("optional is empty"));
     *     }
     * </pre>
     *
     * @param fnGetOk 正常结果值提供函数
     * @param err     错误结果值
     * @param <T>     正常结果类型
     * @return {@code R.Ok(ok)} | {@code R.Err(err)} | {@code R.Err(...)}
     */
    static <T> R<T> ofFnCallable(Callable<? extends T> fnGetOk,
                                 Exception err) {
        final boolean errIsNull = err == null;
        if (fnGetOk == null) {
            if (errIsNull) return new Err<>(new NullPointerException("'fnGetOk' and 'err' is null"));
            else return new Err<>(err);
        }
        try {
            T okOrNull = fnGetOk.call(); // or null
            if (okOrNull == null) {
                if (errIsNull)
                    return new Err<>(new NullPointerException("exec 'fnGetOk' return null and 'err' is null"));
                else return new Err<>(err);
            }
            return new Ok<>(okOrNull); // no null
        } catch (Exception execErr) { // on this method this 'execErr' will be ignored
            if (errIsNull) // execErr and given err is null
                return new Err<>(new RuntimeException("'err' is null and exec 'fnGetOk' throw exception", execErr));
            else return new Err<>(err); // given err no null
        }
    }

    /**
     * 尝试执行函数创建"正常结果"<br>
     * 给定“正常结果值提供函数”，若“正常结果值提供函数”不为 {@code null}，则执行其，
     * 若其执行后返回值不为 {@code null}，返回 {@code R.Ok(ok)}，
     * 若“正常结果值提供函数”为 {@code null} 或其执行过程发生异常，或其执行后返回 {@code null} ，
     * 则尝试执行给定的“错误结果值提供函数”，返回 {@code R.Err(err)}，
     * 若“错误结果值提供函数”为 {@code null}，或其执行过程发生异常，或其执行后返回值为 {@code null} ，
     * 则返回 {@code R.Err(NullPointerException)}
     * <pre>
     *     {@code
     *     Optional<Integer> intOpt = Optional.ofNullable(nullableInt);
     *     R<Integer> r = R.ofFnCallable(intOpt::get, IllegalStateException::new);
     *     }
     * </pre>
     *
     * @param fnGetOk  正常结果值提供函数
     * @param fnGetErr 错误结果值提供函数
     * @param <T>      正常结果类型
     * @return {@code R.Ok(ok)} | {@code R.Err(err)} | {@code R.Err(...)}
     */
    static <T> R<T> ofFnCallable(Callable<? extends T> fnGetOk,
                                 Callable<? extends Exception> fnGetErr) {
        if (fnGetOk == null) { // fnGetOk is null try exec fnGetErr
            return ofOk(null, fnGetErr);
        } else {
            // fnGetOk not null
            try {
                T okOrNull = fnGetOk.call(); // 这里可能异常
                return ofOk(okOrNull, fnGetErr); // 这里不会异常
            } catch (Exception eOnFnGetOkExec) {
                // err on fnGetOk exec.
                // so try fnGetErr
                // 到这里那就一的拿不到 ok结果了
                if (fnGetErr == null) //fnGetErr is null
                    return new Err<>(new RuntimeException("'fnGetErr' is null and exec 'fnGetOk' throw exception", eOnFnGetOkExec));
                try {
                    Exception eOfGetNullable = fnGetErr.call(); // exception or return
                    return new Err<>(Objects.requireNonNullElseGet(eOfGetNullable,
                            () -> new RuntimeException("'fnGetErr' return null and exec 'fnGetOk' throw exception", eOnFnGetOkExec)));
                } catch (Exception eOnFnGetErrExec) {
                    // catch exception on fnGetErr exec.
                    // so here is 2 Err
                    RuntimeException rte = new RuntimeException(
                            "exec 'fnGetOk' throw exception(on Suppressed) and exec 'fnGetErr' throw exception",
                            eOnFnGetErrExec);
                    rte.addSuppressed(eOnFnGetOkExec); // suppressed eOnFnGetOkExec
                    return new Err<>(rte);
                }
            }
        }
    }

    /**
     * 尝试执行函数创建"正常结果"<br>
     * 执行给定的函数，当函数执行过程中发生异常err时，该方法将返回 {@code R.Err(err)}，否则返回 {@code R.Ok(ok)},
     * 若给定的函数为 {@code null} 或函数执行结果为 {@code null}, 则返回 {@code R.Err(NullPointException)}
     *
     * @param fn  正常结果值提供函数
     * @param <T> 正常结果类型
     * @return {@code R.Ok(ok)} | {@code R.Err(err)} | {@code R.Err(NullPointException)}
     */
    static <T> R<T> ofFnCallable(Callable<? extends T> fn) {
        if (fn == null) return new Err<>(new NullPointerException("'fn' is null"));
        try {
            return ofOk(fn.call());
        } catch (Exception execFnCallErrOrFnReturnNull) {
            return ofErr(execFnCallErrOrFnReturnNull);
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
     * @throws UnwrapException 当容器对象为错误结果时
     */
    T ok() throws UnwrapException;

    /**
     * 尝试获取正常结果<br>
     * 当容器对象为"错误结果"对象时，该方法返回 {@link Optional#empty()}
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
     * @throws UnwrapException 当容器对象为正常结果时
     */
    Exception err() throws UnwrapException;

    /**
     * 尝试获取错误结果<br>
     * 当容器对象为"正常结果"对象时，该方法返回 {@link Optional#empty()}
     *
     * @return 错误结果Optional
     */
    Optional<Exception> tryErr();

    /**
     * 返回{@link Optional}对象，尝试获取正常结果<br>
     * 当容器对象为"错误结果"对象时，该方法返回 {@link Optional#empty()}
     *
     * @return 正常结果Optional
     * @see #tryOk()
     */
    default Optional<T> toOptional() {
        return tryOk();
    }

    /**
     * 解包该实体以尝试获取 {@link T}<br>
     * 当该实体为 {@link R.Ok } 时能够成功解包并返回 {@link T}，
     * 否则将立即抛出 {@link UnwrapException}，其 cause 为 Exception<br>
     * <pre>
     *     {@code
     *     R<String> rOk = R.ofOk("123");
     *     Assert.eq("123", rOk.unwrap());
     *     R<String> rErr = R.ofErr(new IOException());
     *     Assert.throwE(UnwrapException.class, () -> rErr.unwrap());
     *     Assert.isTrue(IOException.class == rErr.err().getClass());
     *     }
     * </pre>
     *
     * @return 正常结果值（或直接抛出异常）
     * @throws UnwrapException 当该实体为“错误结果”时
     * @see #unwrap(Function)
     * @see #unwrapOr(Object)
     * @see #unwrapOrGet(Supplier)
     */
    T unwrap() throws UnwrapException;

    /**
     * 解包该实体以尝试获取 {@link T}<br>
     * 当该实体为 {@link R.Ok } 时能够成功解包并返回 {@link T}，
     * 否则将立即尝试调用函数将 {@link R.Err} 中的 {@link Exception} 作为函数输入，并抛出函数返回的异常<br>
     * <pre>
     *     {@code
     *     R<String> rOk = R.ofOk("123");
     *     Assert.eq("123", rOk.unwrap(IllegalStateException::new));
     *     R<String> rErr = R.ofErr(new IOException());
     *     Assert.throwE(IOException.class, () -> rErr.unwrap(e -> e));
     *     }
     * </pre>
     *
     * @param <X>     要抛出的异常类型
     * @param ifNotOk 异常转换函数
     * @return 正常结果值（或直接抛出指定异常）
     * @throws X                    当没有“正常结果值”时
     * @throws NullPointerException 当没有“正常结果值”且异常提供函数为空时
     * @apiNote 异常提供函数可以以方法引用形式引用异常的 {@code new Exception(Throwable)} 构造，形如：
     * {@code IllegalStateException::new}
     * @see #unwrap()
     * @since 0.0.5
     */
    <X extends Throwable> T unwrap(Function<? super Exception, ? extends X> ifNotOk) throws X;


    /**
     * 解包该实体以尝试获取 {@link T}，若失败则返回给定的默认值<br>
     * 给定的默认值可以为 {@code null}
     *
     * @param or 默认值
     * @return 正常结果值 | 默认值
     * @see #unwrap()
     * @see #unwrapOrGet(Supplier)
     */
    T unwrapOr(T or);

    /**
     * 解包该实体以尝试获取 {@link T}，若失败则运行给定函数返回函数结果<br>
     * 给定函数的返回值可以为 {@code null}
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
    void ifErr(Consumer<? super Exception> fnAccErr);

    /**
     * 当“正常结果值”存在，则返回只有一个元素的 {@link Stream}，
     * 否则，返回空流 {@link Stream#empty()}
     *
     * @return 包含一个“正常结果值”的流 | 空流 {@link Stream#empty()}
     * @apiNote 该API适用于下场景（该方法会将 {@link Exception} 丢弃，使用方需考量适用与否）：
     * <pre>{@code
     *     Stream<R<T>> os = ...
     *     Stream<T> s = os.flatMap(R::stream)
     * }</pre>
     * @since 0.0.5
     */
    Stream<T> stream();

    /**
     * 尝试对当前可能存在的 {@code Ok} 做判定，若函数为 {@code null}、判定未通过、判定过程发生异常,
     * 返回 {@code R.Err(...)}，否则返回 {@code this}<br>
     * 若当前为 {@code Err}，则不会调用判定函数<br>
     * 任何导致判定函数不能返回 {@code true} 或无法达到返回 {@code true} 的状态都会使该方法返回 {@code R.Err(...)}
     *
     * @param fnTest 判定函数
     * @return {@code this} | {@code R.Err(...)}
     * @throws NullPointerException 当当前为 {@code R.Ok} 且给定的函数为 {@code null}
     */
    default R<T> filter(Predicate<? super T> fnTest) {
        if (isOk()) {
            Objects.requireNonNull(fnTest, "'fnTest' is null");
            try {
                boolean test = fnTest.test(ok());
                if (!test) throw new IllegalStateException("'fnTest' test failed");
                return this;
            } catch (Exception e) {
                return new Err<>(e);
            }
        } else {
            return this;
        }
    }

    /*
    不应当有：
    default boolean test(Predicate<? super T> fnTest)
    filter方法形容的主语是 R 本身，即：不符合/异常/则变为 R.Err，
    而 test 返回的 二值（true/false）无法完整描述当前 R 状态（可能符合/不符合/异常/不存在），
    需以某种手段或约定归约才可有此，其次，该test也像是语法糖，遂废弃，不应有该
    20250526: 这也启发了--- 应有约定：
    若对R类型实例进行不符合R类型本身语义的操作时（R仅应形容其载荷的Ok）发生异常（并不代指R变为Err，而是代指在操作R的当前上下文发生异常）
    则说明外界给定的操作不合法（比如给定的函数为null）？--- 该已记录在 R class doc 里，作为约定。
    是否实现 recover match peek consume 需后续再做观察
     */


    /**
     * 根据给定函数，尝试转换“正常结果值”并返回新的 {@link R} 对象<br>
     * 当前实体为“正常结果”时，给定的函数将被执行以进行“正常结果值”的转换，
     * 当前实体为“错误结果”时，给定的函数将不会被执行<br>
     * 若函数执行结果为 {@code null}，则 {@link R} 类型为 {@link R.Err} 携带 {@link NullPointerException},
     * 若函数执行过程抛出异常，则 {@link R} 类型为 {@link R.Err} 携带被抛出的异常
     *
     * @param fn  正常结果值转换函数
     * @param <U> 新的“正常结果值”类型
     * @return {@code R.Ok(U)} | {@code R.Err(...)} (this)
     * @throws NullPointerException 当当前为 {@code R.Ok} 且给定的函数为 {@code null}
     */
    default <U> R<U> map(Fn<? super T, ? extends U> fn) {
        if (isOk()) {
            Objects.requireNonNull(fn, "'fn' is null");
            try {
                return ofOk(fn.unsafeApply(ok()));
            } catch (Exception e) { // catch maybe fn is null or on apply exception
                return new Err<>(e);
            }
        } else {
            @SuppressWarnings("unchecked")
            R<U> er = (R<U>) this; // 因为为Err，所以可以返回自己
            return er;
        }
    }

    /**
     * 根据给定函数，尝试转换“正常结果值”到“{@link R}对象”，并返回该对象<br>
     * 当前实体为“正常结果”时，给定的函数将被执行以进行“正常结果值”到“结果对象”的转换，
     * 当前实体为“错误结果”时，给定的函数将不会被执行<br>
     * 若函数执行结果为 {@code null}，则 {@link R} 类型为 {@link R.Err} 携带 {@link NullPointerException},
     * 若函数执行过程抛出异常，则 {@link R} 类型为 {@link R.Err} 携带被抛出的异常
     *
     * @param fn  正常结果值到 {@link R} 的转换函数
     * @param <U> 新的“正常结果值”类型
     * @return {@code R.Ok(U)} | {@code R.Err(...)} (this)
     * @throws NullPointerException 当当前为 {@code R.Ok} 且给定的函数为 {@code null}
     */
    @SuppressWarnings("unchecked")
    default <U> R<U> flatMap(Fn<? super T, ? extends R<? extends U>> fn) {
        if (isOk()) {
            Objects.requireNonNull(fn, "'fn' is null");
            try {
                R<U> apply = (R<U>) fn.unsafeApply(ok());
                if (apply == null) return new Err<>(new NullPointerException("flatmap fn return null"));
                else return apply;
            } catch (Exception execErr) {
                return new Err<>(execErr);
            }
        } else {
            // 因为为Err，所以可以返回自己
            return (R<U>) this;
        }
    }

    /**
     * 根据给定函数，尝试转换“错误结果值”并返回新的 {@link R} 对象<br>
     * 当前实体为“错误结果”时，给定的函数将被执行以进行“错误结果值”的转换，
     * 当前实体为“正常结果”时，给定的函数将不会被执行
     *
     * @param fn 错误结果值转换函数
     * @return 新的结果对象
     * @throws NullPointerException 当当前为 {@code R.Err} 且给定的函数为 {@code null}
     * @apiNote 因为该方法语义是明确的将原有 R 内的异常转为新的异常类型，
     * 遂当给定的函数转换过程 抛出异常时，该异常不会被 R 持有，而是直接抛出
     */
    default R<T> mapErr(Function<? super Exception, ? extends Exception> fn) {
        if (isErr()) { // to do? maybe on apply will exception?
            return ofErr(Objects.requireNonNull(fn, "'fn' is null").apply(err()));
        } else {
            return this;
        }
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
     * @param <T> 正确结果值类型
     * @see R
     */
    record Ok<T>(T ok) implements R<T>, Serializable {
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
        public Optional<T> tryOk() {
            return Optional.of(ok);
        }

        @Override
        public Exception err() throws UnwrapException {
            throw new UnwrapException("not found 'err' value");
        }

        @Override
        public Optional<Exception> tryErr() {
            return Optional.empty();
        }

        @Override
        public T unwrap() throws UnwrapException {
            return ok;
        }

        @Override
        public T unwrapOr(T or) {
            return ok;
        }

        @Override
        public T unwrapOrGet(Supplier<? extends T> fnGet) {
            return ok;
        }

        @Override
        public <X extends Throwable> T unwrap(Function<? super Exception, ? extends X> ifNotOk) {
            return ok;
        }

        @Override
        public void ifOk(Consumer<? super T> fnAccOk) {
            fnAccOk.accept(ok());
        }

        @Override
        public void ifErr(Consumer<? super Exception> fnAccErr) {
            // pass... do nothing because I'm an 'Ok'
        }

        @Override
        public Stream<T> stream() {
            return Stream.of(ok);
        }
    }

    /**
     * 错误结果
     *
     * @param <T> 正确结果值类型
     * @see R
     */
    record Err<T>(Exception err) implements R<T>, Serializable {
        @Serial
        private static final long serialVersionUID = 333L;

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
        public T ok() throws UnwrapException {
            throw new UnwrapException("not found 'ok' value");
        }

        @Override
        public Optional<T> tryOk() {
            return Optional.empty();
        }

        @Override
        public Optional<Exception> tryErr() {
            return Optional.of(err);
        }

        @Override
        public Exception err() {
            return err;
        }

        @Override
        public T unwrap() throws UnwrapException {
            throw new UnwrapException(err);
        }

        @Override
        public T unwrapOr(T or) {
            return or;
        }

        @Override
        public T unwrapOrGet(Supplier<? extends T> fnGet) {
            return Objects.requireNonNull(fnGet, "fnGet is null").get();
        }

        @Override
        public <X extends Throwable> T unwrap(Function<? super Exception, ? extends X> ifNotOk) throws X {
            throw ifNotOk.apply(err);
        }

        @Override
        public void ifOk(Consumer<? super T> fnAccOk) {
            // pass... do nothing because I'm an 'Err'
        }

        @Override
        public void ifErr(Consumer<? super Exception> fnAccErr) {
            fnAccErr.accept(err());
        }

        @Override
        public Stream<T> stream() {
            return Stream.empty();
        }
    }

    /**
     * 表达 {@link R} 类型解包时的异常
     */
    class UnwrapException extends RuntimeException {
        public UnwrapException(String message) {
            super(message);
        }

        public UnwrapException(String message, Throwable cause) {
            super(message, cause);
        }

        public UnwrapException(Throwable cause) {
            super(cause);
        }
    }
}


