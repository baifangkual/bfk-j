package io.github.baifangkual.bfk.j.mod.core.lang;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;

/**
 * @author baifangkual
 * @since 2025/5/18
 */
@SuppressWarnings({"CommentedOutCode", "Convert2MethodRef"})
public class TreeTest {

    @Test
    public void test() {

        List<Line<Integer>> lines = List.of(
                Line.of(1, 2),
                Line.of(2, 3),
                Line.of(3, 4),
                Line.of(1, 5),
                Line.of(5, 6),
                Line.of(10, 11)
        );
        Tree<Integer> integerTree = Tree.ofRoots(
                List.of(1, 10),
                num -> lines.stream()
                        .filter(l -> l.begin().equals(num))
                        .map(Line::end)
                        .toList(),
                Tree.NodeType.bidirectionalNode,
                LinkedList::new
        );
        Tree<Integer> integerTree1 = Tree.tryOfLines(lines).unwrap();
        Assertions.assertEquals(integerTree.displayString(), integerTree1.displayString());
        //System.out.println(integerTree.displayString(0));
        //System.out.println("====================");
        //System.out.println(integerTree1.displayString(integerTree1.depth()));
    }

    @Test
    public void test2() {
        List<Line<Integer>> lines = List.of(
                Line.of(111, 2),
                Line.of(2, 3),
                Line.of(3, 4),
                Line.of(111, 5),
                Line.of(5, 6),
                Line.of(111, 44),
                Line.of(2, 35),
                Line.of(2, 49)
        );
        Tree<Integer> tree = Tree.ofRoots(
                List.of(111),
                num -> lines.stream()
                        .filter(l -> l.begin().equals(num))
                        .map(Line::end)
                        .toList(),
                Tree.NodeType.bidirectionalNode,
                (i1, i2) -> Integer.compare(i1, i2),
                LinkedList::new,
                e -> true,
                e -> true,
                Integer.MAX_VALUE
        );
        StringJoiner pre = new StringJoiner("\n");
        pre.add("DFS PRE ORDER:");
        tree.dfsPreOrder(n -> pre.add(n.toString()));
        StringJoiner post = new StringJoiner("\n");
        post.add("DFS POST ORDER:");
        tree.dfsPostOrder(n -> post.add(n.toString()));
        Assertions.assertNotEquals(pre.toString(), post.toString());
        StringJoiner bfs = new StringJoiner("\n");
        bfs.add("BFS:");
        tree.bfs(n -> bfs.add(n.toString()));
        Assertions.assertNotEquals(bfs.toString(), post.toString());
        Assertions.assertNotEquals(pre.toString(), bfs.toString());
//        System.out.println(STF.f("tree depth: {}", tree.depth()));
//        System.out.println(tree.displayString());
//        System.out.println(tree.displayString(Integer.MAX_VALUE,
//                n -> STF.f("[dep: {}, value: {}]", n.currentDepth(), n.value())));
//        System.out.println(bfs);
//        System.out.println(pre);
//        System.out.println(post);
    }

    @Test
    public void test3() {
        List<Line<Integer>> lines = List.of(
                Line.of(111, 2),
                Line.of(2, 3),
                Line.of(3, 4),
                Line.of(111, 5),
                Line.of(5, 6),
                Line.of(111, 44),
                Line.of(2, 35),
                Line.of(2, 49)
        );
        Tree<Integer> tree = Tree.tryOfLines(lines).unwrap();
        List<Line<Integer>> unwrap = tree.tryToLines(LinkedList::new, Tree.Node::data).unwrap();
        Assertions.assertEquals(lines.size(), unwrap.size());
        // sort
        Comparator<Line<Integer>> lc = Comparator.comparingInt(Line::begin);
        Comparator<Line<Integer>> lineComparator = lc.thenComparingInt(Line::end);
        List<Line<Integer>> sortInLines = lines.stream().sorted(lineComparator).toList();
        List<Line<Integer>> sortOutLines = unwrap.stream().sorted(lineComparator).toList();
        Assertions.assertEquals(sortInLines, sortOutLines);
//        System.out.println(sortSource);
//        System.out.println(sortTarget);

    }

    @Test
    public void test4() {
        NoEqAndHash<Integer> int111 = NoEqAndHash.ofValue(111);
        NoEqAndHash<Integer> int2 = NoEqAndHash.ofValue(2);
        NoEqAndHash<Integer> int3 = NoEqAndHash.ofValue(3);
        NoEqAndHash<Integer> int4 = NoEqAndHash.ofValue(4);
        NoEqAndHash<Integer> int5 = NoEqAndHash.ofValue(5);
        NoEqAndHash<Integer> int6 = NoEqAndHash.ofValue(6);
        NoEqAndHash<Integer> int44 = NoEqAndHash.ofValue(44);
        NoEqAndHash<Integer> int35 = NoEqAndHash.ofValue(35);
        NoEqAndHash<Integer> int49 = NoEqAndHash.ofValue(49);
        List<Line<NoEqAndHash<Integer>>> lines = List.of(
                Line.of(int111, int2),
                Line.of(int2, int3),
                Line.of(int3, int4),
                Line.of(int111, int5),
                Line.of(int5, int6),
                Line.of(int111, int44),
                Line.of(int2, int35),
                Line.of(int2, int49)
        );
        Tree<NoEqAndHash<Integer>> tree = Tree.tryOfLines(lines).unwrap();
        List<Line<NoEqAndHash<Integer>>> unwrap = tree.tryToLines(LinkedList::new, Tree.Node::data).unwrap();
        Assertions.assertEquals(lines.size(), unwrap.size());
        // sort
        Comparator<Line<NoEqAndHash<Integer>>> lc = Comparator.comparingInt(l -> l.begin().value);
        Comparator<Line<NoEqAndHash<Integer>>> lineComparator = lc.thenComparingInt(l -> l.end().value);
        List<Line<NoEqAndHash<Integer>>> sortSource = lines.stream().sorted(lineComparator).toList();
        List<Line<NoEqAndHash<Integer>>> sortTarget = unwrap.stream().sorted(lineComparator).toList();
        Assertions.assertEquals(sortSource, sortTarget);
//        System.out.println(tree.displayString());
//        System.out.println(sortSource);
//        System.out.println(sortTarget);

    }

    @Test
    public void test5() {

        List<Line<NoEqAndHash<Integer>>> lines = List.of(
                Line.of(NoEqAndHash.ofValue(111), NoEqAndHash.ofValue(2)),
                Line.of(NoEqAndHash.ofValue(2), NoEqAndHash.ofValue(3)),
                Line.of(NoEqAndHash.ofValue(3), NoEqAndHash.ofValue(4)),
                Line.of(NoEqAndHash.ofValue(111), NoEqAndHash.ofValue(5)),
                Line.of(NoEqAndHash.ofValue(5), NoEqAndHash.ofValue(6)),
                Line.of(NoEqAndHash.ofValue(111), NoEqAndHash.ofValue(44)),
                Line.of(NoEqAndHash.ofValue(2), NoEqAndHash.ofValue(35)),
                Line.of(NoEqAndHash.ofValue(2), NoEqAndHash.ofValue(49))
        );
        Tree<NoEqAndHash<Integer>> tree = Tree.tryOfLines(lines).unwrap();
        List<Line<NoEqAndHash<Integer>>> unwrap = tree.tryToLines(LinkedList::new, Tree.Node::data).unwrap();
        Assertions.assertEquals(lines.size(), unwrap.size());
        // sort
        Comparator<Line<NoEqAndHash<Integer>>> lc = Comparator.comparingInt(l -> l.begin().value);
        Comparator<Line<NoEqAndHash<Integer>>> lineComparator = lc.thenComparingInt(l -> l.end().value);
        List<Line<NoEqAndHash<Integer>>> sortSource = lines.stream().sorted(lineComparator).toList();
        List<Line<NoEqAndHash<Integer>>> sortTarget = unwrap.stream().sorted(lineComparator).toList();
        Assertions.assertEquals(sortSource, sortTarget);
//        System.out.println(tree.displayString());
//        System.out.println(sortSource);
//        System.out.println(sortTarget);
    }

    @Test
    public void test6() {
        List<Line<Integer>> lines = List.of(
                Line.of(1, 2),
                Line.of(2, 3),
                Line.of(3, 4),
                Line.of(1, 5),
                Line.of(5, 6)
        );
        Tree<Integer> tree = Tree.ofRoots(
                List.of(1),
                num -> lines.stream()
                        .filter(l -> l.begin().equals(num))
                        .map(Line::end)
                        .toList(),
                Tree.NodeType.bidirectionalNode,
                LinkedList::new
        );
        Tree<Integer> integerTree1 = Tree.tryOfLines(lines).unwrap();
        Assertions.assertEquals(tree.displayString(), integerTree1.displayString());
        System.out.println(tree.displayString());
    }

    static class NoEqAndHash<I> {
        I value;

        static <E> NoEqAndHash<E> ofValue(E value) {
            NoEqAndHash<E> o = new NoEqAndHash<>();
            o.value = value;
            return o;
        }

        @Override
        public String toString() {
            return "NoEqAndHash[" + value + ']';
        }
    }


}
