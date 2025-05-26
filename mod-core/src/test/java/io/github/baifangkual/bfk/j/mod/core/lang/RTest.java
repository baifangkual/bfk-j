package io.github.baifangkual.bfk.j.mod.core.lang;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author baifangkual
 * @since 2025/5/3
 */
@SuppressWarnings({"OptionalGetWithoutIsPresent", "OptionalAssignedToNull", "ConstantValue", "DataFlowIssue", "ThrowableNotThrown"})
public class RTest {

    @Test
    public void test01() {

        R<String> r1 = R.ofOk(null, new Exception());
        Assertions.assertTrue(r1.isErr());
        R<Integer> r2 = R.ofOk(1, IllegalAccessException::new);
        Assertions.assertEquals(R.ofOk(1), r2);
        Optional<String> strOpt = Optional.empty();
        R<String> r3 = R.ofFnCallable(strOpt::get);
        Assertions.assertTrue(r3.isErr());
        R<String> r4 = R.ofFnCallable(strOpt::get, IllegalAccessException::new);
        Assertions.assertEquals(IllegalAccessException.class, r4.tryErr().orElseThrow().getClass());
    }

    @Test
    public void test02() {
        Assertions.assertThrows(R.UnwrapException.class, () -> R.ofOk(null, IllegalArgumentException::new).unwrap());
    }

    @Test
    public void test04() {
        Assertions.assertThrows(NullPointerException.class, () -> R.ofOk(null).unwrap(Function.identity()));
    }

    @Test
    public void test05() {
        R<Integer> r = R.ofOk(5);
        Assertions.assertThrows(R.UnwrapException.class, r::err);
        Assertions.assertTrue(r.toOptional().isPresent());
        Assertions.assertTrue(r.tryOk().isPresent());
        Assertions.assertTrue(r.tryErr().isEmpty());
    }

    @Test
    public void test06() {
        R<String> err = R.ofErr(new IOException("err"));
        Assertions.assertThrows(R.UnwrapException.class, err::ok);
    }

    @Test
    public void test07() {
        R<String> err = R.ofErr(new IllegalStateException("err"));
        Assertions.assertThrows(R.UnwrapException.class, err::unwrap);
        R<Integer> r = R.ofOk(5);
        Integer unwrap = r.unwrap();
        Assertions.assertEquals(5, unwrap);
        R.Err<String> err1 = R.ofErr(null);
        String ss = err1.unwrapOr("ss");
        Assertions.assertEquals("ss", ss);
        String s55 = err1.unwrapOrGet(() -> "55");
        Assertions.assertEquals("55", s55);
    }

    @Test
    public void test08() {
        R<String> r1 = R.ofOk(null, IOException::new);
        Assertions.assertThrows(IllegalStateException.class, () -> r1.unwrap(IllegalStateException::new));
        R<Integer> r2 = R.ofOk(1);
        Assertions.assertEquals(1, r2.unwrap(IllegalStateException::new));
    }

    @Test
    public void test09() {
        Stream<R<Integer>> s = Stream.of(
                R.ofOk(1),
                R.ofOk(2),
                R.ofOk(3),
                R.ofErr(new NullPointerException()),
                R.ofOk(5)
        );
        Stream<Integer> integerStream = s.flatMap(R::stream);
        List<Integer> list = integerStream.toList();
        Assertions.assertEquals(List.of(1, 2, 3, 5), list);
    }

    @Test
    public void test10() {
        Optional<Integer> i = Optional.of(1);
        R<Integer> r = R.ofOptional(i);
        Assertions.assertTrue(r.isOk());
        Assertions.assertEquals(1, r.toOptional().get());
        Assertions.assertEquals(1, r.unwrap());
    }

    @Test
    public void test11() {
        Optional<String> sOpt = Optional.empty();
        R<String> r = R.ofOptional(sOpt);
        Assertions.assertTrue(r.isErr());
        Assertions.assertThrows(R.UnwrapException.class, r::unwrap);
        Assertions.assertEquals(NoSuchElementException.class, r.err().getClass());
    }

    @Test
    public void test12() {
        Optional<String> selfNull = null;
        R<String> r = R.ofOptional(selfNull);
        Assertions.assertTrue(r.isErr());
        Assertions.assertEquals(NullPointerException.class, r.err().getClass());
    }

    @Test
    public void test13() {
        CompletableFuture<String> fOk = CompletableFuture.completedFuture("ok");
        CompletableFuture<String> fErr = CompletableFuture.failedFuture(new IllegalStateException("err"));
        CompletableFuture<String> fSelfNull = null;
        CompletableFuture<String> fRNull = CompletableFuture.completedFuture(null);
        R<String> rOk = R.ofFuture(fOk);
        R<String> rErr = R.ofFuture(fErr);
        R<String> rSelfNull = R.ofFuture(fSelfNull);
        R<String> rRNull = R.ofFuture(fRNull);
        Assertions.assertEquals("ok", rOk.unwrap());
        Assertions.assertThrows(R.UnwrapException.class, rErr::unwrap);
        //System.out.println(rErr.unwrap());
        Assertions.assertEquals(IllegalStateException.class, rErr.err().getCause().getClass());
        //System.out.println(rSelfNull.unwrap());
        Assertions.assertThrows(NullPointerException.class, () -> {
            throw rSelfNull.err();
        });
        //System.out.println(rRNull.unwrap());
        Assertions.assertEquals(NullPointerException.class, rRNull.err().getClass());

    }




}
