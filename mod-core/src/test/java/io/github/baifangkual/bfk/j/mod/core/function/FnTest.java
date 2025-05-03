package io.github.baifangkual.bfk.j.mod.core.function;

import io.github.baifangkual.bfk.j.mod.core.exception.PanicException;
import io.github.baifangkual.bfk.j.mod.core.model.R;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.function.Function;

/**
 * @author baifangkual
 * create time 2025/5/3
 */
@SuppressWarnings("ConstantValue")
public class FnTest {


    @Test
    public void test01() {
        Fn<String, String> fn = (s) -> "fnMod: " + s;
        R<String, Exception> abc = fn.apply("abc");
        System.out.println(abc);
        Function<String, R<String, Exception>> safe = fn.toSafe();
        R<String, Exception> def = safe.apply("def");
        System.out.println(def);

    }

    @Test
    public void test02() {
        Fn<String, String> fn = (s) -> {
            if (true) {
                throw new ClassCastException("message");
            }
            return s;
        };

        R<String, Exception> abc = fn.apply("abc");
        System.out.println(abc);
    }

    @Test
    public void test03() throws Exception {
        Fn<String, String> fn = (s) -> {
            if (true) {
                throw new ClassCastException("message");
            }
            return s;
        };
        Assertions.assertThrows(ClassCastException.class, () -> {
            String abc = fn.unsafeApply("abc");
            System.out.println(abc);
        });
    }

    @Test
    public void test04() {
        Fn<String, String> fn = (s) -> {
            if (true) {
                throw new ClassCastException("message");
            }
            return s;
        };
        Assertions.assertThrows(PanicException.class, () -> {
            String abc = fn.toUnsafe().apply("abc");
            System.out.println(abc);
        });
    }

    @Test
    public void test05() {
        Fn<String, String> fn = (s) -> {
            if (true) {
                throw new ClassCastException("message");
            }
            return s;
        };
        Assertions.assertThrows(ClassCastException.class, () -> {
            String abc = fn.toSneaky().apply("abc");
            System.out.println(abc);
        });
    }

    @Test
    public void test06() {
        Fn<String, String> fn = (s) -> {
            if (true) {
                throw new ClassCastException("message");
            }
            return s;
        };
        Optional<String> optional = fn.toSafe().apply("abc").toOptional();
        System.out.println(optional);
    }

}
