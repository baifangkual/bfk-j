package io.github.baifangkual.bfk.j.mod.core.lang;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author baifangkual
 * @since 2025/6/3
 */
@SuppressWarnings({"StringEquality", "SimplifiableAssertion"})
public class IndexedTest {


    @Test
    public void test() {

        List<String> l26 = List.of("a", "b", "c", "d", "e", "f", "g", "h",
                "i", "j", "k", "l", "m", "n", "o", "p",
                "q", "r", "s", "t", "u", "v", "w", "x", "y", "z");
        List<Indexed<String>> i26l = Indexed.toIndexedList(l26);
        for (Indexed<String> is : i26l) {
            Assertions.assertTrue(is.value() == l26.get(is.index()));
//            System.out.println(is);
        }
    }

    @Test
    public void test2() {
        List<Integer> l = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        Stream<Indexed<Integer>> istream = Indexed.toIndexedStream(l.stream());
        istream.forEach(i -> {
            Assertions.assertEquals(i.index(),  i.value());
            //System.out.println(i);
        });
    }

    @Test
    public void test3() {
        // 模拟无限流
        Stream<Integer> intLoopStream = IntStream.iterate(0, i -> i + 1)
                .boxed();
        Indexed.toIndexedStream(intLoopStream)
                .limit(1000)
                .forEach(i -> {
                    Assertions.assertEquals(i.index(),  i.value());
                    System.out.println(i);
                });
    }

    @Test
    public void test4() {
        // 模拟无限并发流
        Stream<Integer> intLoopStream = IntStream.iterate(0, i -> i + 1)
                .boxed();
        Indexed.toIndexedStream(intLoopStream)
                .parallel()
                .limit(1000)
                .forEach(i -> {
                    Assertions.assertEquals(i.index(),  i.value());
                    System.out.println(i);
                });
    }
}
