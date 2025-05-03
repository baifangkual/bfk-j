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
 * 异常类工具简单构建，利用java泛型擦除屏蔽预检异常声明,为使栈展开不繁琐，该类里方法不应定位为互相调用，应确保该线程安全<br>
 * 使用说明：
 * <pre>
 *     {@code
 *     // 当条件成立，默认行为：立即抛出new RuntimeException("空字符串")，
 *     Panic.errIf("".isblank(), "空字符串");
 *     // 当条件成立，默认行为：立即抛出指定的异常，不携带errMessage，要求指定的异常拥有无参构造
 *     Panic.errIf("".eq(""), IllegalStateException::new);
 *     // 当条件成立，默认行为：立即抛出指定的异常，携带errMessage，要求指定的异常拥有有参构造(String errMessage)
 *     Panic.errIf("abc".contains("b"), IllegalArgumentException::new, "字符串中包含b");
 *     // 当条件成立，默认行为：立即抛出指定的异常，携带errMessage，要求指定的异常用哟有参构造(String errMessage)
 *     Panic.errIf("abc".contains("b"), IllegalArgumentException::new, "字符串{}中包含{}", "abc",'b');
 *     // 进行显式抛出预检异常的操作而又不想声明try/cache或不想将预检异常向方法throws声明时
 *     Panic.runOrPanic(() -> System.out.println(new URL("test://")));
 *     // 同上，区别是操作有返回值
 *     URL url = Panic.runOrPanic(() -> new URL("test://"));
 *     }
 * </pre>
 * 或可参考各方法上更为详细的说明及示例<br>
 * @see #realIf(boolean, String)
 * @see #realIf(boolean, Supplier)
 * @see #realIf(boolean, Function, String)
 * @see #realIf(boolean, Function, String, Object...)
 * @see #runOrThrowReal(Run)
 * @see #runOrThrowReal(Sup)
 */
public final class Err {
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

    public static void realIf(boolean expression, String message) {
        if (expression) {
            throwReal(new RuntimeException(message));
        }
    }

    public static void realIf(boolean expression, Supplier<? extends Exception> trSup) {
        if (expression) {
            throwReal(trSup.get());
        }
    }

    public static void realIf(boolean expression, Function<? super String, ? extends Exception> trFn,
                              String message) {
        if (expression) {
            throwReal(trFn.apply(message));
        }
    }

    public static void realIf(boolean expression, Function<? super String, ? extends Exception> trFn,
                              String msgTemplate, Object... msgArgs) {
        if (expression) {
            throwReal(trFn.apply(STF.f(msgTemplate, msgArgs)));
        }
    }

    /**
     * 根据给定的表达式布尔值结果选择是否抛出异常，屏蔽java预检异常与运行时异常的方法声明
     *
     * @param expression      表达式，true 抛出异常，false 不抛出异常
     * @param trFn            函数，异常对象的构造方法的方法引用，要求该异常对象的构造方法能够接受一个str类型或其父类型的参数，返回一个异常实例
     * @param msgTemplate     异常对象的消息的模板，参数使用 "{}" 表示
     * @param msgArgsSupplier 函数，能够提供填充异常对象的消息模板的参数，
     *                        之所以使用该参数提供函数是为了优化一些场景，
     *                        与{@link Err#realIf(boolean, Function, String, Object...)}方法区分开
     *                        当调用该方法的作用域中没有直接对应的msgTemplate中所需参数的值且构造该值需要复杂的流程或大对象时，
     *                        该方法将优化此场景，即仅当确定要抛出异常时才会执行参数的构造
     */
    public static void realIf(boolean expression, Function<? super String, ? extends Exception> trFn,
                              String msgTemplate, Supplier<? extends Object[]> msgArgsSupplier) {
        if (expression) {
            throwReal(trFn.apply(STF.f(msgTemplate, msgArgsSupplier.get())));
        }
    }

    public static void panicIf(boolean expression, String message) {
        if (expression) {
            throwPanic(new RuntimeException(message));
        }
    }

    public static void panicIf(boolean expression, Supplier<? extends Exception> trSup) {
        if (expression) {
            throwPanic(trSup.get());
        }
    }

    public static void panicIf(boolean expression, Function<? super String, ? extends Exception> trFn,
                               String message) {
        if (expression) {
            throwPanic(trFn.apply(message));
        }
    }

    public static void panicIf(boolean expression, Function<? super String, ? extends Exception> trFn,
                               String msgTemplate, Object... msgArgs) {
        if (expression) {
            throwPanic(trFn.apply(STF.f(msgTemplate, msgArgs)));
        }
    }

    /**
     * 根据给定的表达式布尔值结果选择是否抛出异常，屏蔽java预检异常与运行时异常的方法声明
     *
     * @param expression      表达式，true 抛出异常，false 不抛出异常
     * @param trFn            函数，异常对象的构造方法的方法引用，要求该异常对象的构造方法能够接受一个str类型或其父类型的参数，返回一个异常实例
     * @param msgTemplate     异常对象的消息的模板，参数使用 "{}" 表示
     * @param msgArgsSupplier 函数，能够提供填充异常对象的消息模板的参数，
     *                        之所以使用该参数提供函数是为了优化一些场景，
     *                        与{@link Err#realIf(boolean, Function, String, Object...)}方法区分开
     *                        当调用该方法的作用域中没有直接对应的msgTemplate中所需参数的值且构造该值需要复杂的流程或大对象时，
     *                        该方法将优化此场景，即仅当确定要抛出异常时才会执行参数的构造
     */
    public static void panicIf(boolean expression, Function<? super String, ? extends Exception> trFn,
                               String msgTemplate, Supplier<? extends Object[]> msgArgsSupplier) {
        if (expression) {
            throwPanic(trFn.apply(STF.f(msgTemplate, msgArgsSupplier.get())));
        }
    }


    /**
     * 悄悄摸摸执行可能发生预检异常的语句并且不声明异常<br>
     * <pre>
     *     {@code
     *        try {
     *             byte[] bytes = Files.readAllBytes(Path.of("/xxx"));
     *         } catch (IOException e) {
     *             throw e;
     *         }
     *         // 上述语句同下，区别是不会因为 e 为预检异常而需要向当前方法作用域内显式声明throws
     *         byte[] bytes = Panic.runOrPanic(() -> Files.readAllBytes(Path.of("/xxx")));
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
     * 悄悄摸摸执行可能发生预检异常的语句并且不声明异常<br>
     * <pre>
     *     {@code
     *        try {
     *             Files.writeString(Path.of("/xxx"), "xxx");
     *         } catch (IOException e) {
     *             throw e;
     *         }
     *         //上述语句同下，区别是不会因为 e 为预检异常而需要向当前方法作用域内显式声明throws
     *         Panic.runOrPanic(() -> Files.writeString(Path.of("/xxx"), "xxx"));
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
     * 悄悄摸摸执行可能发生预检异常的语句并且不声明异常<br>
     * <pre>
     *     {@code
     *        try {
     *             byte[] bytes = Files.readAllBytes(Path.of("/xxx"));
     *         } catch (IOException e) {
     *             throw e;
     *         }
     *         // 上述语句同下，区别是不会因为 e 为预检异常而需要向当前方法作用域内显式声明throws
     *         byte[] bytes = Panic.runOrPanic(() -> Files.readAllBytes(Path.of("/xxx")));
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
     * 悄悄摸摸执行可能发生预检异常的语句并且不声明异常<br>
     * <pre>
     *     {@code
     *        try {
     *             Files.writeString(Path.of("/xxx"), "xxx");
     *         } catch (IOException e) {
     *             throw e;
     *         }
     *         //上述语句同下，区别是不会因为 e 为预检异常而需要向当前方法作用域内显式声明throws
     *         Panic.runOrPanic(() -> Files.writeString(Path.of("/xxx"), "xxx"));
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
