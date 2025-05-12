package io.github.baifangkual.bfk.j.mod.core.model;

import io.github.baifangkual.bfk.j.mod.core.exception.ResultUnwrapException;
import io.github.baifangkual.bfk.j.mod.core.exception.ResultWrapException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.function.Function;

/**
 * @author baifangkual
 * @since 2025/5/3
 */
@SuppressWarnings({"CommentedOutCode"})
public class RTest {

    @Test
    public void test01() {

        R<Object, Integer> objectIntegerR = R.of(null, 1);
//        System.out.println(objectIntegerR);
        R<Integer, Object> integerObjectR = R.of(1, null);
//        System.out.println(integerObjectR);
    }

    @Test
    public void test02() {
        Assertions.assertThrows(ResultWrapException.class, () -> R.of(null, null));
    }

    @Test
    public void test03() {
        Assertions.assertThrows(ResultWrapException.class, () -> R.of(1, 1));
    }

    @Test
    public void test04() {
        Assertions.assertThrows(ResultWrapException.class, () -> R.ofOk(null));
    }

    @Test
    public void test05() {
        R<Integer, Object> integerObjectR = R.ofOk(5);
        boolean ok = integerObjectR.isOk();
        boolean err = integerObjectR.isErr();
//        System.out.println(STF.f("r: isOk:{}, isErr:{}", ok, err));
//        Integer ok1 = integerObjectR.ok();
//        System.out.println(ok1);
        Assertions.assertThrows(ResultUnwrapException.class, integerObjectR::err);

//        Optional<Integer> optional = integerObjectR.toOptional();
//        System.out.println(optional);
    }

    @Test
    public void test06() {
        R<String, IOException> err = R.ofErr(new IOException("err"));
//        System.out.println(err);
        boolean ok = err.isOk();
        boolean err1 = err.isErr();
//        System.out.println(STF.f("r: isErr:{}, isOk:{}", err1, ok));
        Assertions.assertThrows(ResultUnwrapException.class, err::ok);
        IOException err2 = err.err();
//        System.out.println(err2);
//        Optional<String> optional = err.toOptional();
//        System.out.println(optional);
    }

    @Test
    public void test07() {
        R<String, Exception> err = R.ofErr(new IllegalStateException("err"));
        Assertions.assertThrows(IllegalStateException.class, err::unwrap);
        R<Integer, Exception> integerObjectR = R.ofOk(5);
        Integer unwrap = integerObjectR.unwrap();
//        System.out.println(unwrap);
        Assertions.assertEquals(5, unwrap);
    }

    @Test
    public void test08() {
        // 成功情况
        Result<Integer, String> success = divide(10, 2);
//        System.out.println("Success: " + success.unwrap()); // 5

        // 错误情况
        Result<Integer, String> failure = divide(10, 0);
//        System.out.println("Failure: " + failure.unwrapOr(-1)); // -1

        // 链式操作
        Result<Integer, String> result = divide(100, 2)
                .map(x -> x * 3)
                .andThen(x -> divide(x, 5));

//        System.out.println("Chained result: " + result.unwrap()); // 30
    }

    // 模拟可能失败的操作
    private static Result<Integer, String> divide(int a, int b) {
        if (b == 0) {
            return new Result.Err<>("Division by zero");
        }
        return new Result.Ok<>(a / b);
    }

    public sealed interface Result<T, E> permits Result.Ok, Result.Err {
        // 成功情况的记录
        record Ok<T, E>(T value) implements Result<T, E> {
        }

        // 错误情况的记录
        record Err<T, E>(E error) implements Result<T, E> {
        }

        // 解包值，如果错误则抛出异常
        default T unwrap() {
            if (this instanceof Ok<T, E> ok) {
                return ok.value();
            } else if (this instanceof Err<T, E> err) {
                throw new RuntimeException("Called unwrap on an Err value: " + err.error());
            }
            throw new RuntimeException("Unknown Result variant");
        }

        // 解包值或提供默认值
        default T unwrapOr(T defaultValue) {
            return this instanceof Ok<T, E> ok ? ok.value() : defaultValue;
        }

        // 检查是否是Ok
        default boolean isOk() {
            return this instanceof Ok<T, E>;
        }

        // 检查是否是Err
        default boolean isErr() {
            return this instanceof Err<T, E>;
        }

        // 映射Ok值
        default <U> Result<U, E> map(Function<T, U> mapper) {
            return this instanceof Ok<T, E> ok
                    ? new Ok<>(mapper.apply(ok.value()))
                    : new Err<>(((Err<T, E>) this).error());
        }

        // 映射Err值
        default <F> Result<T, F> mapErr(Function<E, F> mapper) {
            return this instanceof Err<T, E> err
                    ? new Err<>(mapper.apply(err.error()))
                    : new Ok<>(((Ok<T, E>) this).value());
        }

        // 链式操作
        default <U> Result<U, E> andThen(Function<T, Result<U, E>> mapper) {
            return this instanceof Ok<T, E> ok
                    ? mapper.apply(ok.value())
                    : new Err<>(((Err<T, E>) this).error());
        }
    }

}
