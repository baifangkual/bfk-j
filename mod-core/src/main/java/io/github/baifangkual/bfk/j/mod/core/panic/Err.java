package io.github.baifangkual.bfk.j.mod.core.panic;

import io.github.baifangkual.bfk.j.mod.core.exception.PanicException;
import io.github.baifangkual.bfk.j.mod.core.fmt.STF;
import io.github.baifangkual.bfk.j.mod.core.function.Run;
import io.github.baifangkual.bfk.j.mod.core.function.Sup;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author baifangkual
 * create time 2024/4/15
 * <p>
 * <b>异常校验工具类</b>
 * 利用java泛型擦除屏蔽预检异常声明,为使栈展开不繁琐，该类里方法不应定位为互相调用，应确保该线程安全<br>
 * 该类中方法签名中 realxxx 表示直接返回相应的异常（运行时或预检异常），panicxxx 表示返回以{@link PanicException}类型包装的相应异常<br>
 * 使用说明：
 * <pre>
 *     {@code
 *     // 当条件成立，默认行为：立即抛出new RuntimeException("空字符串")，
 *     Err.realIf("".isblank(), "空字符串");
 *     // 当条件成立，默认行为：立即抛出指定的异常，不携带errMessage，要求指定的异常拥有无参构造
 *     Err.realIf("".eq(""), IllegalStateException::new);
 *     // 当条件成立，默认行为：立即抛出指定的异常，携带errMessage，要求指定的异常拥有有参构造(String errMessage)
 *     Err.realIf("abc".contains("b"), IllegalArgumentException::new, "字符串中包含b");
 *     // 当条件成立，默认行为：立即抛出指定的异常，携带errMessage，要求指定的异常用哟有参构造(String errMessage)
 *     Err.realIf("abc".contains("b"), IllegalArgumentException::new, "字符串{}中包含{}", "abc",'b');
 *     // 进行显式抛出预检异常的操作而又不想声明try/cache或不想将预检异常向方法throws声明时
 *     Err.runOrThrowReal(() -> System.out.println(new URL("test://")));
 *     // 同上，区别是操作有返回值
 *     URL url = Err.runOrThrowReal(() -> new URL("test://"));
 *     }
 * </pre>
 * 或可参考各方法上更为详细的说明及示例<br>
 * @see #realIf(boolean, String)
 * @see #realIf(boolean, Supplier)
 * @see #realIf(boolean, Function, String)
 * @see #realIf(boolean, Function, String, Object...)
 * @see #realIf(boolean, Function, String, Supplier)
 * @see #panicIf(boolean, String)
 * @see #panicIf(boolean, Supplier)
 * @see #panicIf(boolean, Function, String)
 * @see #panicIf(boolean, Function, String, Object...)
 * @see #panicIf(boolean, Function, String, Supplier)
 * @see #runOrThrowReal(Run)
 * @see #runOrThrowReal(Sup)
 * @see #runOrThrowPanic(Run)
 * @see #runOrThrowPanic(Sup)
 */
public final class Err {
    /**
     * 不允许构建工具类实例
     */
    private Err() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * 该调用将立即抛出指定异常，即使异常为预检异常，当前作用域也不需要显式声明预检异常，
     * 骗过编译器，无需将预检异常显式转为运行时异常
     *
     * @param err 给定要抛出的异常
     */
    @SuppressWarnings("unchecked")
    public static <ERR extends Exception> void throwReal(Exception err) throws ERR {
        throw (ERR) err;
    }

    /**
     * 该调用将立即将异常包装为{@link PanicException} 并抛出，抛出的该异常为运行时异常
     *
     * @param err 给定要抛出的异常
     */
    public static void throwPanic(Exception err) {
        throw PanicException.wrap(err);
    }

    /**
     * 当expression为真时，抛出{@link RuntimeException}并携带信息
     *
     * @param expression 布尔表达式
     * @param msg        携带的信息
     */
    public static void realIf(boolean expression, String msg) {
        if (expression) {
            throwReal(new RuntimeException(msg));
        }
    }

    /**
     * 当expression为真时，抛出指定直接异常，包括运行时和预检异常
     *
     * @param expression 布尔表达式
     * @param trSup      异常生产者函数
     */
    public static void realIf(boolean expression, Supplier<? extends Exception> trSup) {
        if (expression) {
            throwReal(trSup.get());
        }
    }

    /**
     * 当expression为真时，抛出指定直接异常，包括运行时和预检异常
     *
     * @param expression 布尔表达式
     * @param trFn       异常生产函数
     * @param msg        异常携带信息
     */
    public static void realIf(boolean expression, Function<? super String, ? extends Exception> trFn,
                              String msg) {
        if (expression) {
            throwReal(trFn.apply(msg));
        }
    }

    /**
     * 当expression为真时，抛出指定直接异常，包括运行时和预检异常
     *
     * @param expression 布尔表达式
     * @param trFn       异常生产函数
     * @param msgTemp    异常信息模板（形如：“err: msg:{}”)
     * @param msgArgs    填充异常信息模板的参数列表（按顺序填充异常信息模板中的"{}")
     */
    public static void realIf(boolean expression, Function<? super String, ? extends Exception> trFn,
                              String msgTemp, Object... msgArgs) {
        if (expression) {
            throwReal(trFn.apply(STF.f(msgTemp, msgArgs)));
        }
    }

    /**
     * 根据给定的表达式布尔值结果选择是否抛出异常，屏蔽java预检异常与运行时异常的方法声明
     *
     * @param expression 表达式，true 抛出异常，false 不抛出异常
     * @param trFn       函数，异常对象的构造方法的方法引用，要求该异常对象的构造方法能够接受一个str类型或其父类型的参数，返回一个异常实例
     * @param msgTemp    异常对象的消息的模板，参数使用 "{}" 表示
     * @param msgArgsSup 函数，能够提供填充异常对象的消息模板的参数，
     *                   之所以使用该参数提供函数是为了优化一些场景，
     *                   与{@link Err#realIf(boolean, Function, String, Object...)}方法区分开
     *                   当调用该方法的作用域中没有直接对应的msgTemplate中所需参数的值且构造该值需要复杂的流程或大对象时，
     *                   该方法将优化此场景，即仅当确定要抛出异常时才会执行参数的构造
     */
    public static void realIf(boolean expression, Function<? super String, ? extends Exception> trFn,
                              String msgTemp, Supplier<? extends Object[]> msgArgsSup) {
        if (expression) {
            throwReal(trFn.apply(STF.f(msgTemp, msgArgsSup.get())));
        }
    }

    /**
     * 当expression为真时，抛出{@link PanicException}并携带信息
     *
     * @param expression 布尔表达式
     * @param msg        携带的信息
     */
    public static void panicIf(boolean expression, String msg) {
        if (expression) {
            throwPanic(new RuntimeException(msg));
        }
    }

    /**
     * 当expression为真时，抛出{@link PanicException}并携带信息
     *
     * @param expression 布尔表达式
     * @param trSup      异常提供函数，提供的异常将被包装为{@link PanicException}类型
     */
    public static void panicIf(boolean expression, Supplier<? extends Exception> trSup) {
        if (expression) {
            throwPanic(trSup.get());
        }
    }

    /**
     * 当expression为真时，抛出{@link PanicException}并携带信息
     *
     * @param expression 布尔表达式
     * @param trFn       异常提供函数，提供的异常将被包装为{@link PanicException}类型
     * @param msg        异常携带信息
     */
    public static void panicIf(boolean expression, Function<? super String, ? extends Exception> trFn,
                               String msg) {
        if (expression) {
            throwPanic(trFn.apply(msg));
        }
    }

    /**
     * 当expression为真时，抛出{@link PanicException}并携带信息
     *
     * @param expression 布尔表达式
     * @param trFn       异常提供函数，提供的异常将被包装为{@link PanicException}类型
     * @param msgTemp    异常信息模板（形如：“err: msg:{}”)
     * @param msgArgs    填充异常信息模板的参数列表（按顺序填充异常信息模板中的"{}")
     */
    public static void panicIf(boolean expression, Function<? super String, ? extends Exception> trFn,
                               String msgTemp, Object... msgArgs) {
        if (expression) {
            throwPanic(trFn.apply(STF.f(msgTemp, msgArgs)));
        }
    }

    /**
     * 根据给定的表达式布尔值结果选择是否抛出异常，屏蔽java预检异常与运行时异常的方法声明，抛出{@link PanicException}并携带信息
     *
     * @param expression 表达式，true 抛出异常，false 不抛出异常
     * @param trFn       函数，异常对象的构造方法的方法引用，要求该异常对象的构造方法能够接受一个str类型或其父类型的参数，返回一个异常实例
     * @param msgTemp    异常对象的消息的模板，参数使用 "{}" 表示
     * @param msgArgsSup 函数，能够提供填充异常对象的消息模板的参数，
     *                   之所以使用该参数提供函数是为了优化一些场景，
     *                   与{@link Err#realIf(boolean, Function, String, Object...)}方法区分开
     *                   当调用该方法的作用域中没有直接对应的msgTemplate中所需参数的值且构造该值需要复杂的流程或大对象时，
     *                   该方法将优化此场景，即仅当确定要抛出异常时才会执行参数的构造
     */
    public static void panicIf(boolean expression, Function<? super String, ? extends Exception> trFn,
                               String msgTemp, Supplier<? extends Object[]> msgArgsSup) {
        if (expression) {
            throwPanic(trFn.apply(STF.f(msgTemp, msgArgsSup.get())));
        }
    }


    /**
     * 执行给定的可能抛出异常的函数，当发生异常时，直接抛出相应异常，即使该异常为预检异常<br>
     * <pre>
     *     {@code
     *        try {
     *             byte[] bytes = Files.readAllBytes(Path.of("/xxx"));
     *         } catch (IOException e) {
     *             throw e;
     *         }
     *         // 上述语句同下，区别是不会因为 e 为预检异常而需要向当前方法作用域内显式声明throws
     *         byte[] bytes = Err.runOrThrowReal(() -> Files.readAllBytes(Path.of("/xxx")));
     *     }
     * </pre>
     *
     * @param sup 可执行语句,可能异常，异常时直接抛出，对预检和运行时异常皆可
     */
    public static <R> R runOrThrowReal(Sup<? extends R> sup) {
        try {
            return sup.unsafeGet();
        } catch (Exception e) {
            throwReal(e);
        }
        // 不会执行到此处，仅为编译通过
        throw new RuntimeException();
    }

    /**
     * 执行给定的可能抛出异常的函数，当发生异常时，直接抛出相应异常，即使该异常为预检异常<br>
     * <pre>
     *     {@code
     *        try {
     *             Files.writeString(Path.of("/xxx"), "xxx");
     *         } catch (IOException e) {
     *             throw e;
     *         }
     *         //上述语句同下，区别是不会因为 e 为预检异常而需要向当前方法作用域内显式声明throws
     *         Err.runOrThrowReal(() -> Files.writeString(Path.of("/xxx"), "xxx"));
     *     }
     * </pre>
     *
     * @param run 可执行语句,可能异常，异常时直接抛出，对预检和运行时异常皆可
     */
    public static void runOrThrowReal(Run run) {
        try {
            run.unsafeRun();
        } catch (Exception e) {
            throwReal(e);
        }
    }


    /**
     * 执行给定的可能抛出异常的函数，当发生异常时，包装相应异常为{@link PanicException}并抛出<br>
     * <pre>
     *     {@code
     *        try {
     *             byte[] bytes = Files.readAllBytes(Path.of("/xxx"));
     *         } catch (IOException e) {
     *             throw e;
     *         }
     *         // 上述语句同下，区别是不会因为 e 为预检异常而需要向当前方法作用域内显式声明throws
     *         byte[] bytes = Err.runOrThrowPanic(() -> Files.readAllBytes(Path.of("/xxx")));
     *     }
     * </pre>
     *
     * @param sup 可执行语句,可能异常，异常时直接抛出，对预检和运行时异常皆可
     */
    public static <R> R runOrThrowPanic(Sup<? extends R> sup) {
        try {
            return sup.unsafeGet();
        } catch (Exception e) {
            throwPanic(e);
        }
        // 不会执行到此处，仅为编译通过
        throw new RuntimeException();
    }

    /**
     * 执行给定的可能抛出异常的函数，当发生异常时，包装相应异常为{@link PanicException}并抛出<br>
     * <pre>
     *     {@code
     *        try {
     *             Files.writeString(Path.of("/xxx"), "xxx");
     *         } catch (IOException e) {
     *             throw e;
     *         }
     *         //上述语句同下，区别是不会因为 e 为预检异常而需要向当前方法作用域内显式声明throws
     *         Err.runOrThrowPanic(() -> Files.writeString(Path.of("/xxx"), "xxx"));
     *     }
     * </pre>
     *
     * @param run 可执行语句,可能异常，异常时直接抛出，对预检和运行时异常皆可
     */
    public static void runOrThrowPanic(Run run) {
        try {
            run.unsafeRun();
        } catch (Exception e) {
            throwPanic(e);
        }
    }


}
