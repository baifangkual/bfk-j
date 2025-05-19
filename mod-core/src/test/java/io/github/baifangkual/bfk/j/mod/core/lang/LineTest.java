package io.github.baifangkual.bfk.j.mod.core.lang;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

/**
 * @author baifangkual
 * @since 2025/5/16
 */
public class LineTest {


    @Test
    public void test() {

        R<LinkedList<List<Integer>>, RuntimeException> r = Line.orderDAGQueue(List.of(
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
        R<LinkedList<List<Integer>>, RuntimeException> r = Line.orderDAGQueue(List.of(
                Line.of(1, 2),
                Line.of(2, 3),
                Line.of(3, 1)
        ));

        Assertions.assertThrows(R.UnwrapException.class, r::unwrap);
    }
}
