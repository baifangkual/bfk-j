package cn.bfk.j.mod.core.exception;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author baifangkual
 * create time 2024/11/15
 * <p>
 * 表示程序恐慌异常，必须对引发恐慌的实际异常原因进行包装，
 * 除了将预检异常转化为运行时异常，其他行为不应发生改变，遂该类的其他行为都应将委托至实际的异常原因<br>
 * 该类的构造中要求给定一个{@link Throwable}，也即导致恐慌的实际异常原因，因该类型的表意，
 * 即导致Panic发生的异常实体一定不为垂悬引用，遂该Panic的构造中，{@link #wrap(Throwable)}要求给定的{@link Throwable}一定不为null，
 * 否则该类的构造将直接抛出{@link NullPointerException}<br>
 * 该类的出现时是为了将java异常体系中细分的预检/运行时异常进行统一<br>
 * 对该类调用{@link #toString()}或{@link #printStackTrace()}等，将显示实际异常原因的异常信息，该类型仅会添加{@link #PANIC_PREFIX}至
 * 异常信息说明及栈回溯前等<br>
 */
public final class Panic extends RuntimeException implements Serializable {

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
     * 将实际异常包装为{@link Panic} 运行时异常<br>
     * 20241118-该方法的方法签名不应为“from”,因为该方法的行为算是将其他异常进行包装，其他异常仍在该{@link Panic}内部持有引用，
     * 并且外显仍为原有的其他异常，遂该方法不为从一种类型转话为另一种类型，方法的释义类似于“包装、代理”等，遂方法签名设定为"wrap“
     *
     * @param realCause 实际异常实例
     * @return PanicException instance
     */
    public static Panic wrap(Throwable realCause) {
        if (realCause == null) {
            throw new NullPointerException("realCause is null, panic not found realCause!");
        }
        return new Panic(realCause);
    }

    /**
     * 将该类型构造使用私有作用域，使该类型唯一构造方式使用{@link #wrap(Throwable)} 方法
     */
    private Panic(Throwable selfPanicRealCause) {
        // 调用父的构造，writableStackTrace 为 false，即当前RuntimeE的子类构造时不调用 fillInStackTrace
        // 并且，该的 enableSuppression 也为 false，又因为 addSuppressed 和 getSuppressed 均为 final 方法，
        // 即外界不能向该异常添加次级异常，也从该类型获取不到次级异常
        super(null, null, false, false);
        this.selfPanicRealCause = selfPanicRealCause;
    }

    /**
     * 实际的异常cause
     */
    private Throwable selfPanicRealCause() {
        return this.selfPanicRealCause;
    }

    @Override
    public String getMessage() {
        return selfPanicRealCause().getMessage();
    }

    /**
     * 将自身的cause委托至{@link #selfPanicRealCause()}的cause
     */
    @Override
    public Throwable getCause() {
        // 因为 selfPanicRealCause 的 getCause 有 synchronized，遂该处重写的方法可无需要求 synchronized
        return selfPanicRealCause().getCause();
    }


    @Override
    public String getLocalizedMessage() {
        return selfPanicRealCause().getLocalizedMessage();
    }

    @Override
    public Throwable initCause(Throwable cause) {
        // 因为 selfPanicRealCause 的 initCause 有 synchronized，遂该处重写的方法可无需要求 synchronized
        return selfPanicRealCause().initCause(cause);
    }


    @Override
    public String toString() {
        return PANIC_PREFIX + selfPanicRealCause().toString();
    }

    /**
     * 将自身的栈追踪委托至实际异常的栈追踪
     */
    @Override
    public StackTraceElement[] getStackTrace() {
        return selfPanicRealCause().getStackTrace();
    }

    @Override
    public void setStackTrace(StackTraceElement[] stackTrace) {
        selfPanicRealCause().setStackTrace(stackTrace);
    }

    @Override
    public Throwable fillInStackTrace() {
        // 因为 selfPanicRealCause 的 fillInStackTrace 有 synchronized，遂该处重写的方法可无需要求 synchronized
        return selfPanicRealCause().fillInStackTrace();
    }
}
