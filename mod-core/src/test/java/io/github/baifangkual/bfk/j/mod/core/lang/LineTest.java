package io.github.baifangkual.bfk.j.mod.core.lang;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * @author baifangkual
 * @since 2025/5/16
 */
public class LineTest {


    @Test
    public void test() {

        R<Queue<Set<Integer>>, RuntimeException> r = Line.forOrder(List.of(
                Line.of(1, 2),
                Line.of(2, 3),
                Line.of(3, 4),
                Line.of(1, 5),
                Line.of(5, 3)
        ));
        Assertions.assertEquals("[[1], [2, 5], [3], [4]]", r.unwrap().toString());
    }

    @Test
    public void test2() {
        R<Queue<Set<Integer>>, RuntimeException> r = Line.forOrder(List.of(
                Line.of(1, 2),
                Line.of(2, 3),
                Line.of(3, 1)
        ));

        Assertions.assertThrows(R.UnwrapException.class, r::unwrap);
    }
}
