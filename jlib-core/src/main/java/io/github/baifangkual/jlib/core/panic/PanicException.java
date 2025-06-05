package io.github.baifangkual.jlib.core.panic;

import java.io.Serial;
import java.io.Serializable;

/**
 * <b>恐慌异常</b><br>
 * 表示程序恐慌异常，必须对引发恐慌的实际异常原因进行包装，
 * 除了将预检异常转化为运行时异常，其他行为不应发生改变，遂该类的其他行为都应将委托至实际的异常原因<br>
 * 该类的构造中要求给定一个{@link Throwable}，也即导致恐慌的实际异常原因，因该类型的表意，
 * 即导致Panic发生的异常实体一定不为垂悬引用，遂该Panic的构造中，{@link #wrap(Throwable)}要求给定的{@link Throwable}一定不为null，
 * 否则该类的构造将直接抛出{@link NullPointerException}<br>
 * 该类的出现时是为了将java异常体系中细分的预检/运行时异常进行统一<br>
 * 对该类调用{@link #toString()}或{@link #printStackTrace()}等，将显示实际异常原因的异常信息，该类型仅会添加{@value #PANIC_PREFIX}至
 * 异常信息说明及栈回溯前
 *
 * @author baifangkual
 * @since 2024/11/15 v0.0.3
 */
public final class PanicException extends RuntimeException implements Serializable {

    /**
     * jdk序列化标记字段，异疑事，吾亦试，疑久忆旧罢已龄
     */
    @Serial
    private static final long serialVersionUID = -1145141919810L;
    /**
     * 恐慌异常{@link #toString()}前缀
     */
    private static final String PANIC_PREFIX = "!!!PANIC!!! ";
    /**
     * 实际异常原因的引用，虽然该类型构造中通过超类引用调用了{@link RuntimeException#RuntimeException(Throwable)}，
     * 从而该类型的超类的{@link Throwable#getCause()}可获取到实际的异常原因，
     * 但因为超类中{@link Throwable#getCause()}方法使用了synchronized关键字，
     * 且该类中其他方法均需要实际的异常，遂若使用超类的{@link Throwable#getCause()}获取实际异常实体将导致大部分方法低效，
     * 遂这里直接持有异常实际原因实体的引用，供该类中方法使用<br>
     * 20250214-该类型的构造中已不调用{@link RuntimeException#RuntimeException(Throwable)}, 而调用
     * {@link RuntimeException#RuntimeException(String, Throwable, boolean, boolean)}，遂该处的对原始的实际异常的引用将为唯一引用
     */
    private final Throwable selfPanicRealCause;

    /**
     * 将实际异常包装为{@link PanicException} 运行时异常
     *
     * @param realCause 实际异常实例
     * @return PanicException实例，该实例为运行时异常
     */
    public static PanicException wrap(Throwable realCause) {
        if (realCause == null) {
            throw new NullPointerException("realCause is null, panicException not found realCause!");
        }
        return new PanicException(realCause);
    }

    /**
     * 将该类型构造使用私有作用域，使该类型唯一构造方式使用{@link #wrap(Throwable)} 方法
     *
     * @param selfPanicRealCause 实际的异常
     */
    private PanicException(Throwable selfPanicRealCause) {
        // 调用父的构造，writableStackTrace 为 false，即当前RuntimeE的子类构造时不调用 fillInStackTrace
        // 并且，该的 enableSuppression 也为 false，又因为 addSuppressed 和 getSuppressed 均为 final 方法，
        // 即外界不能向该异常添加次级异常，也从该类型获取不到次级异常
        super(null, null, false, false);
        this.selfPanicRealCause = selfPanicRealCause;
    }

    /**
     * 实际的异常cause
     *
     * @return 返回实际的异常
     */
    private Throwable selfPanicRealCause() {
        return this.selfPanicRealCause;
    }

    /**
     * 委托至实际异常的信息
     *
     * @return 实际异常携带的信息
     */
    @Override
    public String getMessage() {
        return selfPanicRealCause().getMessage();
    }

    /**
     * 将自身的cause委托至{@link #selfPanicRealCause()}的cause
     *
     * @return 实际异常的cause
     */
    @Override
    public Throwable getCause() {
        // 因为 selfPanicRealCause 的 getCause 有 synchronized，遂该处重写的方法可无需要求 synchronized
        return selfPanicRealCause().getCause();
    }

    /**
     * 将自身的LocalizedMessage委托至实际异常的LocalizeMessage
     *
     * @return 实际异常的LocalizedMessage
     */
    @Override
    public String getLocalizedMessage() {
        return selfPanicRealCause().getLocalizedMessage();
    }

    /**
     * 修改实际异常的cause实例
     *
     * @param cause the cause (which is saved for later retrieval by the
     *              {@link #getCause()} method).  (A {@code null} value is
     *              permitted, and indicates that the cause is nonexistent or
     *              unknown.)
     * @return 返回实际的异常 {@link #selfPanicRealCause()} 引用的异常实例
     */
    @Override
    public Throwable initCause(Throwable cause) {
        // 因为 selfPanicRealCause 的 initCause 有 synchronized，遂该处重写的方法可无需要求 synchronized
        return selfPanicRealCause().initCause(cause);
    }

    /**
     * 返回实际异常的toString信息（添加前缀{@value #PANIC_PREFIX})
     *
     * @return 实际异常的toString信息
     */
    @Override
    public String toString() {
        return PANIC_PREFIX + selfPanicRealCause().toString();
    }

    /**
     * 将自身的栈追踪委托至实际异常的栈追踪
     *
     * @return 实际异常的栈追踪
     */
    @Override
    public StackTraceElement[] getStackTrace() {
        return selfPanicRealCause().getStackTrace();
    }

    /**
     * 向实际异常设置栈追踪
     *
     * @param stackTrace the stack trace elements to be associated with
     *                   this {@code Throwable}.  The specified array is copied by this
     *                   call; changes in the specified array after the method invocation
     *                   returns will have no affect on this {@code Throwable}'s stack
     *                   trace.
     */
    @Override
    public void setStackTrace(StackTraceElement[] stackTrace) {
        selfPanicRealCause().setStackTrace(stackTrace);
    }

    /**
     * 填充执行堆栈跟踪。此方法在此对象中 Throwable 记录有关当前线程的堆栈帧的当前状态的信息
     * 如果 this Throwable 的堆栈跟踪 不可写，则调用此方法无效
     *
     * @return this
     */
    @Override
    public Throwable fillInStackTrace() {
        // 因为 selfPanicRealCause 的 fillInStackTrace 有 synchronized，遂该处重写的方法可无需要求 synchronized
        selfPanicRealCause().fillInStackTrace();
        return this;
    }
}
