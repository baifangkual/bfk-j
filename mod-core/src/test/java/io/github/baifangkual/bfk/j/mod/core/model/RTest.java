package io.github.baifangkual.bfk.j.mod.core.model;

import io.github.baifangkual.bfk.j.mod.core.fmt.STF;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * @author baifangkual
 * create time 2025/5/3
 */
@SuppressWarnings("ThrowablePrintedToSystemOut")
public class RTest {

    @Test
    public void test01() {

        R<Object, Integer> objectIntegerR = R.of(null, 1);
        System.out.println(objectIntegerR);
        R<Integer, Object> integerObjectR = R.of(1, null);
        System.out.println(integerObjectR);
    }

    @Test
    public void test02() {
        Assertions.assertThrows(IllegalStateException.class, () -> R.of(null, null));
    }

    @Test
    public void test03() {
        Assertions.assertThrows(IllegalStateException.class, () -> R.of(1, 1));
    }

    @Test
    public void test04() {
        Assertions.assertThrows(IllegalStateException.class, () -> R.ofOk(null));
    }

    @Test
    public void test05() {
        R<Integer, Object> integerObjectR = R.ofOk(5);
        boolean ok = integerObjectR.isOk();
        boolean err = integerObjectR.isErr();
        System.out.println(STF.f("r: isOk:{}, isErr:{}", ok, err));
        Integer ok1 = integerObjectR.ok();
        System.out.println(ok1);
        Assertions.assertThrows(NoSuchElementException.class, integerObjectR::err);

        Optional<Integer> optional = integerObjectR.toOptional();
        System.out.println(optional);
    }

    @Test
    public void test06() {
        R<String, IOException> err = R.ofErr(new IOException("err"));
        System.out.println(err);
        boolean ok = err.isOk();
        boolean err1 = err.isErr();
        System.out.println(STF.f("r: isErr:{}, isOk:{}", err1, ok));
        Assertions.assertThrows(NoSuchElementException.class, err::ok);
        IOException err2 = err.err();
        System.out.println(err2);
        Optional<String> optional = err.toOptional();
        System.out.println(optional);
    }

    @Test
    public void test07() {
        R<String, Exception> err = R.ofErr(new IllegalStateException("err"));
        Assertions.assertThrows(IllegalStateException.class, err::unwrap);
        R<Integer, Exception> integerObjectR = R.ofOk(5);
        Integer unwrap = integerObjectR.unwrap();
        System.out.println(unwrap);
    }
}
