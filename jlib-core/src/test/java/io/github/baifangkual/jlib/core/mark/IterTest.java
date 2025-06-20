package io.github.baifangkual.jlib.core.mark;

import io.github.baifangkual.jlib.core.lang.Indexed;
import io.github.baifangkual.jlib.core.lang.Line;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author baifangkual
 * @since 2025/6/20
 */
public class IterTest {


    @Test
    public void test01() {

        Line<Integer> l = Line.of(1, 2);

        List<Integer> collect = l.collect(ArrayList::new);
        Assertions.assertEquals(List.of(1, 2), collect);

        Iterator<Long> sIt = l.mappedIterator(i -> i * 10L);
        ArrayList<Long> collect1 = Iter.collect(sIt, ArrayList::new);
        Assertions.assertEquals(List.of(10L, 20L), collect1);

        Iterator<Indexed<Integer>> indexedIterator = l.indexedIterator();
        ArrayList<Indexed<Integer>> collect2 = Iter.collect(indexedIterator, ArrayList::new);
        Assertions.assertEquals(List.of(Indexed.of(0, 1), Indexed.of(1, 2)), collect2);

        List<Indexed<Integer>> c3 = new ArrayList<>(2);
        l.indexedSpliterator().forEachRemaining(c3::add);
        Assertions.assertEquals(List.of(Indexed.of(0, 1), Indexed.of(1, 2)), c3);

    }
}
