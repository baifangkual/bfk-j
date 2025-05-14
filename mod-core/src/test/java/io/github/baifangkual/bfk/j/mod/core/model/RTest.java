package io.github.baifangkual.bfk.j.mod.core.model;

import io.github.baifangkual.bfk.j.mod.core.exception.ResultUnwrapException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author baifangkual
 * @since 2025/5/3
 */
public class RTest {

    @Test
    public void test01() {

        R<String, Integer> r1 = R.ofNullable(null, 1);
        Assertions.assertEquals(R.ofErr(1), r1);
        R<Integer, Exception> r2 = R.ofNullable(1, IllegalAccessException::new);
        Assertions.assertEquals(R.ofOk(1), r2);
        Optional<String> strOpt = Optional.empty();
        R<String, Integer> r3 = R.ofSupplier(strOpt::get, 5);
        Assertions.assertEquals(R.ofErr(5), r3);
        R<String, Integer> r4 = R.ofSupplier(strOpt::get, () -> 5);
        Assertions.assertEquals(R.ofErr(5), r4);
    }

    @Test
    public void test02() {
        Assertions.assertThrows(NullPointerException.class, () -> R.ofNullable(null, null));
    }

    @Test
    public void test04() {
        Assertions.assertThrows(NullPointerException.class, () -> R.ofOk(null));
    }

    @Test
    public void test05() {
        R<Integer, Exception> r = R.ofOk(5);
        Assertions.assertThrows(ResultUnwrapException.class, r::err);
        Assertions.assertTrue(r.toOptional().isPresent());
        Assertions.assertTrue(r.tryOk().isPresent());
        Assertions.assertTrue(r.tryErr().isEmpty());
    }

    @Test
    public void test06() {
        R<String, IOException> err = R.ofErr(new IOException("err"));
        Assertions.assertThrows(ResultUnwrapException.class, err::ok);
    }

    @Test
    public void test07() {
        R<String, Exception> err = R.ofErr(new IllegalStateException("err"));
        Assertions.assertThrows(ResultUnwrapException.class, err::unwrap);
        R<Integer, Exception> r = R.ofOk(5);
        Integer unwrap = r.unwrap();
        Assertions.assertEquals(5, unwrap);
        R.Err<String, Integer> err1 = R.ofErr(5);
        String ss = err1.unwrapOr("ss");
        Assertions.assertEquals("ss", ss);
        String s55 = err1.unwrapOrGet(() -> "55");
        Assertions.assertEquals("55", s55);
    }

    @Test
    public void test08() {
        R<String, Integer> r1 = R.ofNullable(null, 1);
        Assertions.assertThrows(IllegalStateException.class, () -> r1.unwrap(IllegalStateException::new));
        R<Integer, NullPointerException> r2 = R.ofNullable(1);
        Assertions.assertEquals(1, r2.unwrap(IllegalStateException::new));
    }

    @Test
    public void test09() {
        Stream<R<Integer, String>> s = Stream.of(
                R.ofOk(1),
                R.ofOk(2),
                R.ofOk(3),
                R.ofErr("err"),
                R.ofOk(5)
        );
        Stream<Integer> integerStream = s.flatMap(R::stream);
        List<Integer> list = integerStream.toList();
        Assertions.assertEquals(List.of(1, 2, 3, 5), list);
    }


}
