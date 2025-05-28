package io.github.baifangkual.bfk.j.mod.core.func;

import io.github.baifangkual.bfk.j.mod.core.panic.PanicException;
import io.github.baifangkual.bfk.j.mod.core.lang.R;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.function.Function;

/**
 * @author baifangkual
 * @since 2025/5/3
 */
@SuppressWarnings("ConstantValue")
public class FnTest {


    @Test
    public void test01() {
        Fn<String, String> fn = (s) -> "fnMod: " + s;
        R<String> abc = fn.apply("abc");
        Assertions.assertEquals(R.ofOk("fnMod: abc"), abc);
        Function<String, R<String>> safe = fn.toSafe();
        R<String> def = safe.apply("def");
        Assertions.assertEquals(R.ofOk("fnMod: def"), def);
    }

    @Test
    public void test02() {

        final Exception e = new ClassCastException("message");
        Fn<String, String> fn = (s) -> {
            if (true) {
                throw e;
            }
            return s;
        };

        R<String> abc = fn.apply("abc");
//        System.out.println(abc);
        Assertions.assertEquals(R.ofErr(e), abc);
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
        Assertions.assertEquals(Optional.empty(), optional);
    }

    @Test
    public void test07() {
        R<R<Integer>> rr = R.ofFnCallable(() -> R.ofOk(1));
        R<Integer> r = rr.flatMap(Fn.it());
        Assertions.assertEquals(1, r.unwrap());
        R<Integer> map = rr.map(R::unwrap);
        Assertions.assertEquals(1, map.unwrap());
    }

}
