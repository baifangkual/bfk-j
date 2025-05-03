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
@SuppressWarnings("LombokGetterMayBeUsed")
public class R<Ok, Err> implements Serializable {

    @Serial
    private static final long serialVersionUID = 114514L;

    final boolean isOk;
    final Err err;
    final Ok some;

    R(boolean isOk, Ok some, Err err) {
        final boolean haveSome = some != null;
        final boolean haveErr = err != null;
        /* runtime check state */
        if (isOk && haveErr) {
            throw new IllegalStateException("state Ok, but err is no-null, err: " + err);
        } else if (!isOk && !haveErr) {
            throw new IllegalStateException("state Err, but err is null");
        } else if (isOk && !haveSome) {
            throw new IllegalStateException("state Ok, but some is null");
        } else if (!isOk && haveSome) {
            throw new IllegalStateException("state Err, but some is no-null, some: " + some);
        }

        this.isOk = isOk;
        this.err = err;
        this.some = some;
    }

    R(Ok nullableSome, Err nullableErr) {
        this(nullableSome != null, nullableSome, nullableErr);
    }

    public static <Some, E> R<Some, E> ofOk(Some nonnullSome) {
        return new R<>(true, nonnullSome, null);
    }

    public static <Some, E> R<Some, E> ofErr(E nonnullErr) {
        return new R<>(false, null, nonnullErr);
    }

    public static <Some, E> R<Some, E> of(Some nullableSome, E nullableErr) {
        return new R<>(nullableSome, nullableErr);
    }

    public boolean isOk() {
        return isOk;
    }

    public boolean isErr() {
        return !isOk;
    }

    public Ok ok() {
        if (!isOk)
            throw new NoSuchElementException("state Err, some: " + some + ", err: " + err);
        return some;
    }

    public Err err() {
        if (isOk)
            throw new NoSuchElementException("state Ok, some: " + some + ", err: " + err);
        return err;
    }

    public Optional<Ok> toOptional() {
        return isOk ? Optional.of(some) : Optional.empty();
    }

    /**
     * 直接解包该{@link R}容器对象，当结果为{@link Ok}时正常返回，否则将立即抛出运行时异常
     *
     * @return {@link Ok} or throwErr
     */
    public Ok unwrap() {
        if (isOk) {
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

    @Override
    public String toString() {
        return isOk ? STF.f("R[Ok({})]", some) : STF.f("R[Err({})]", err);
    }
}
