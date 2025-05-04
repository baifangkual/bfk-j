package io.github.baifangkual.bfk.j.mod.core.model;

import io.github.baifangkual.bfk.j.mod.core.exception.PanicException;
import io.github.baifangkual.bfk.j.mod.core.fmt.STF;

import java.io.Serial;
import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * @author baifangkual
 * create time 2024/6/18
 * <b>结果容器对象（Result）</b>
 * 一个容器对象 不可变对象 线程安全<br>
 * 标识可能操作失败的逻辑的结果，若操作成功，则some必不为null并且 err为null，
 * 若操作失败，则some必定为null并且err必不为null，
 * 其中Err为操作失败情况下载荷的数据类型<br>
 * 该类型的引用应当永远不为null，该类型的引用应当始终指向一个{@link R}, 该类型应当表达可能出错的操作逻辑结果
 */
public class R<Ok, Err> implements Serializable {

    @Serial
    private static final long serialVersionUID = 114514L;
    /**
     * 存储“异常结果”
     */
    final Err err;
    /**
     * 存储“正常结果”
     */
    final Ok some;

    /**
     * 私有构造，不允许外界直接调用构造函数，携带的“正常”和“异常”结果对象均可能为空，但不会同时为空或同时不为空
     *
     * @param isOk         明确表达容器对象状态是否“正常”
     * @param nullableSome 携带的“正常结果”对象，可能为空
     * @param nullableErr  携带的“异常结果”对象，可能为空
     * @throws IllegalStateException 当给定的参数非法时，会抛出非法状态异常，参数非法状态是指给定的“some”和“err”同时为null或同时不为null
     */
    R(boolean isOk, Ok nullableSome, Err nullableErr) {
        final boolean haveSome = nullableSome != null;
        final boolean haveErr = nullableErr != null;
        /* runtime check state */
        if (isOk && haveErr) {
            throw new IllegalStateException("state Ok, but err is no-null, err: " + nullableErr);
        } else if (!isOk && !haveErr) {
            throw new IllegalStateException("state Err, but err is null");
        } else if (isOk && !haveSome) {
            throw new IllegalStateException("state Ok, but some is null");
        } else if (!isOk && haveSome) {
            throw new IllegalStateException("state Err, but some is no-null, some: " + nullableSome);
        }

        this.err = nullableErr;
        this.some = nullableSome;
    }

    /**
     * 私有构造，不允许外界调用，根据给定的参数构造“正常”或“异常”结果，携带的“正常”和“异常”结果对象均可能为空，但不会同时为空或同时不为空
     *
     * @param nullableSome 携带的“正常结果”对象，可能为空
     * @param nullableErr  携带的“异常结果”对象，可能为空
     * @throws IllegalStateException 当给定的参数非法时，会抛出非法状态异常，参数非法状态是指给定的“some”和“err”同时为null或同时不为null
     */
    R(Ok nullableSome, Err nullableErr) {
        this(nullableSome != null, nullableSome, nullableErr);
    }

    /**
     * 给定非空的对象，明确的构造“正常结果”
     *
     * @param nonnullSome 非空对象
     * @param <Some>      携带的正常结果类型
     * @param <E>         携带的异常结果类型
     * @return “正常结果”
     * @throws IllegalStateException 当给定的some为空时
     */
    public static <Some, E> R<Some, E> ofOk(Some nonnullSome) {
        return new R<>(true, nonnullSome, null);
    }

    /**
     * 给定非空对象，明确构造“异常结果”
     *
     * @param nonnullErr 非空异常结果
     * @param <Some>     携带的正常结果类型
     * @param <E>        携带的异常结果类型
     * @return “异常结果”
     * @throws IllegalStateException 当给定的err为空时
     */
    public static <Some, E> R<Some, E> ofErr(E nonnullErr) {
        return new R<>(false, null, nonnullErr);
    }

    /**
     * 给定非空的some或非空的err，构造“正常”或“异常”结果，给定的参数不得同时为null或同时不为null
     *
     * @param some   携带的正常结果 or null
     * @param orErr  携带的异常结果 or null
     * @param <Some> 携带的正常结果类型
     * @param <E>    携带的异常结果类型
     * @return 正常 or 异常结果（取决于给定的参数）
     * @throws IllegalStateException 当给定的some和err同为null或同不为null时
     */
    public static <Some, E> R<Some, E> of(Some some, E orErr) {
        return new R<>(some, orErr);
    }

    /**
     * 返回是否为正常结果的布尔值
     *
     * @return true表示载荷为正常结果，false表示载荷为异常结果
     */
    public boolean isOk() {
        return this.some != null;
    }

    /**
     * 返回是否为异常结果的布尔值
     *
     * @return true表示载荷为异常结果，false表示载荷为正常结果
     */
    public boolean isErr() {
        return this.err != null;
    }

    /**
     * 直接解包容器对象获取其中载荷的正常结果，当容器对象为异常结果对象时，抛出异常
     *
     * @return 载荷的正常结果对象
     * @throws NoSuchElementException 当容器对象为异常结果对象时
     */
    public Ok ok() {
        if (isErr())
            throw new NoSuchElementException("state Err, some: " + some + ", err: " + err);
        return some;
    }

    /**
     * 直接解包容器对象获取其中载荷的异常结果，当容器对象为正常结果对象时，抛出异常
     *
     * @return 载荷的异常结果对象
     * @throws NoSuchElementException 当容器对象为正常结果对象时
     */
    public Err err() {
        if (isOk())
            throw new NoSuchElementException("state Ok, some: " + some + ", err: " + err);
        return err;
    }

    public Optional<Ok> toOptional() {
        return isOk() ? Optional.of(some) : Optional.empty();
    }

    /**
     * 直接解包该{@link R}容器对象，当结果为{@link Ok}时正常返回，否则将立即抛出运行时异常
     *
     * @return {@link Ok} or throwErr
     */
    public Ok unwrap() {
        if (isOk()) {
            return some;
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
                throw new IllegalStateException("state Err, some: " + some + ", err: " + err);
            }
        }
    }

    /**
     * 返回{@link R}的ToString表现形式
     *
     * @return ToString表现形式
     */
    @Override
    public String toString() {
        return isOk() ? STF.f("R[Ok({})]", some) : STF.f("R[Err({})]", err);
    }
}
