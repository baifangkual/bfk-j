package io.github.baifangkual.jlib.core.lang;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.baifangkual.jlib.core.util.Rng;
import io.github.baifangkual.jlib.core.util.Stf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author baifangkual
 * @since 2025/5/18
 */
@SuppressWarnings({
        "CommentedOutCode",
        "Convert2MethodRef",
        "UnnecessaryLocalVariable",
        "StringOperationCanBeSimplified",
        "MismatchedQueryAndUpdateOfCollection"
})
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
                        .toList()
        );
        Tree<Integer> integerTree1 = Tree.ofLines(lines).unwrap();
        Assertions.assertEquals(integerTree.toDisplayStr(), integerTree1.toDisplayStr());
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
                Comparator.comparingInt(i -> i),
                e -> true,
                e -> true,
                Integer.MAX_VALUE
        );
        StringJoiner pre = new StringJoiner("\n");
        pre.add("DFS PRE ORDER:");
        tree.forEachDfsPreOrder(n -> pre.add(n.toString()));
        StringJoiner post = new StringJoiner("\n");
        post.add("DFS POST ORDER:");
        tree.forEachDfsPostOrder(n -> post.add(n.toString()));
        Assertions.assertNotEquals(pre.toString(), post.toString());
        StringJoiner bfs = new StringJoiner("\n");
        bfs.add("BFS:");
        tree.forEachBfs(n -> bfs.add(n.toString()));
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
        Tree<Integer> tree = Tree.ofLines(lines).unwrap();
        List<Line<Integer>> unwrap = tree.toLines(LinkedList::new, Tree.Node::data).unwrap();
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
        Tree<NoEqAndHash<Integer>> tree = Tree.ofLines(lines).unwrap();
        List<Line<NoEqAndHash<Integer>>> unwrap = tree.toLines(LinkedList::new, Tree.Node::data).unwrap();
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
        Tree<NoEqAndHash<Integer>> tree = Tree.ofLines(lines).unwrap();
        List<Line<NoEqAndHash<Integer>>> unwrap = tree.toLines(LinkedList::new, Tree.Node::data).unwrap();
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
                        .toList()
        );
        Tree<Integer> integerTree1 = Tree.ofLines(lines).unwrap();
        Assertions.assertEquals(tree.toDisplayStr(), integerTree1.toDisplayStr());
        //System.out.println(tree.displayString());
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

    @Test
    public void test7() {
        List<Line<Integer>> lines = List.of(
                Line.of(1, 2),
                Line.of(2, 3),
                Line.of(3, 4),
                Line.of(1, 5),
                Line.of(5, 6)
        );
        Tree<Integer> tree = Tree.ofLines(lines).unwrap();
        String beforeIter = tree.toDisplayStr();
        for (Tree.Node<Integer> n : tree) {
            if (n.data() == null) throw new IllegalStateException(); // do nothing...
        }
        String afterIter = tree.toDisplayStr();
        Assertions.assertEquals(beforeIter, afterIter);
//        System.out.println(tree.displayString());
    }


    @Test
    public void test9() {
        List<Line<Integer>> lines = List.of(
                Line.of(1, 2),
                Line.of(2, 3),
                Line.of(3, 4),
                Line.of(1, 5),
                Line.of(5, 6)
        );
        Tree<Integer> tree = Tree.ofLines(lines).unwrap();
        String beforeIter = tree.toDisplayStr();
        Iterator<Tree.Node<Integer>> it = tree.iterator();
        while (it.hasNext()) {
            Tree.Node<Integer> n = it.next();
            if (n.isLeaf()) {
                it.remove(); // will remove 4 and 6
            }
        }
        String afterIter = tree.toDisplayStr();
        List<Line<Integer>> newNo4And6EndList = lines.stream()
                .filter(l -> (!l.end().equals(4)) && (!l.end().equals(6)))
                .toList();
        Tree<Integer> newTree = Tree.ofLines(newNo4And6EndList).unwrap();
        String newIter = newTree.toDisplayStr();
        Assertions.assertNotEquals(beforeIter, afterIter);
        Assertions.assertEquals(afterIter, newIter);
//        System.out.println(beforeIter);
//        System.out.println(afterIter);
//        System.out.println(newIter);
    }

    @Test
    public void test10() {
        List<Line<Integer>> lines = List.of(
                Line.of(1, 2),
                Line.of(2, 3),
                Line.of(3, 4),
                Line.of(1, 5),
                Line.of(5, 6)
        );
        Tree<Integer> tree = Tree.ofLines(lines).unwrap();
        String beforeIter = tree.toDisplayStr();
        Iterator<Tree.Node<Integer>> it = tree.iterator();
        while (it.hasNext()) {
            Tree.Node<Integer> n = it.next();
            if (n.data().equals(2)) {
                it.remove(); // will 2 and childNodes: 3,4
            }
        }
        String afterIter = tree.toDisplayStr();
        List<Line<Integer>> newNo234List = List.of(
                Line.of(1, 5),
                Line.of(5, 6)
        );
        Tree<Integer> newTree = Tree.ofLines(newNo234List).unwrap();
        String newIter = newTree.toDisplayStr();
        Assertions.assertNotEquals(beforeIter, afterIter);
        Assertions.assertEquals(afterIter, newIter);
//        System.out.println(beforeIter);
//        System.out.println(afterIter);
//        System.out.println(newIter);
    }

    @Test
    public void test11() {
        List<Line<Integer>> lines = List.of(
                Line.of(1, 2),
                Line.of(2, 3),
                Line.of(3, 4),
                Line.of(1, 5),
                Line.of(5, 6)
        );
        Tree<Integer> tree = Tree.ofLines(lines).unwrap();
        Tree<Integer> emptyTree = Tree.empty();
        Iterator<Tree.Node<Integer>> iter = tree.iterator();
        while (iter.hasNext()) {
            // fix tree nodeCount and depth modify
            iter.next();
            iter.remove();
        }
        Assertions.assertEquals(emptyTree.toDisplayStr(), tree.toDisplayStr());
    }

    @Test
    public void test12() {
        List<Line<Integer>> lines = List.of(
                Line.of(111, 2),
                Line.of(2, 3),
                Line.of(3, 4),
                Line.of(111, 5),
                Line.of(5, 6),
                Line.of(111, 44),
                Line.of(2, 35),
                Line.of(2, 49),
                Line.of(222, 666),
                Line.of(888, 333),
                Line.of(333, 555)
        );
        Tree<Integer> tree = Tree.ofLines(lines).unwrap();
        Tree<Integer> emptyTree = Tree.empty();
        Iterator<Tree.Node<Integer>> iter = tree.iterator();
        //System.out.println(tree.displayString());
        while (iter.hasNext()) {
            // fix tree nodeCount and depth modify
            Tree.Node<Integer> n = iter.next();
            //System.out.println(n);
            iter.remove();
        }
        Assertions.assertEquals(emptyTree.toDisplayStr(), tree.toDisplayStr());
    }

    @Test
    public void test13() {
        List<Line<Integer>> lines = List.of(
                Line.of(111, 2),
                Line.of(2, 3),
                Line.of(3, 4),
                Line.of(111, 5),
                Line.of(5, 6),
                Line.of(111, 44),
                Line.of(2, 35),
                Line.of(2, 49),
                Line.of(222, 666),
                Line.of(888, 333),
                Line.of(333, 555)
        );
        Tree<Integer> tree = Tree.ofLines(lines).unwrap();
        Tree<Integer> emptyTree = Tree.empty();
//        System.out.println(STF.f("tree: nodeCount: {}, depth: {}", tree.nodeCount(), tree.depth()));
//        System.out.println(tree.displayString());
        int callIterCount = 0;
        List<Integer> deleted = new ArrayList<>();
        List<Integer> iterPeeked = new ArrayList<>();
        while (!tree.isEmpty()) {
            deleted.clear();
            iterPeeked.clear();
            Iterator<Tree.Node<Integer>> iter = tree.iterator();
            int befCount = tree.nodeCount();
            while (iter.hasNext()) {
                // fix tree nodeCount and depth modify
                Tree.Node<Integer> n = iter.next();
                iterPeeked.add(n.data());
                if (n.isLeaf()) {
                    deleted.add(n.data());
                    iter.remove();
                }
            }
            int aftCount = tree.nodeCount();
            Assertions.assertEquals(befCount - aftCount, deleted.size());
            Assertions.assertEquals(iterPeeked.size(), befCount);
//            String peek = iterPeeked.stream().map(Object::toString).collect(Collectors.joining(" -> "));
//            System.out.println(STF.f("callIterCount: {}, result: ", ++callIterCount));
//            System.out.println(STF.f("peek: {}", peek));
//            System.out.println(STF.f("deleted: {}", deleted));
//            System.out.println(STF.f("tree: nodeCount: {}, depth: {}", tree.nodeCount(), tree.depth()));
//            System.out.println(tree.displayString());
        }
        Assertions.assertEquals(emptyTree.toDisplayStr(), tree.toDisplayStr());
    }

    @Test
    public void test14() {
        List<Line<Integer>> lines = List.of(
                Line.of(111, 2),
                Line.of(2, 3),
                Line.of(3, 4),
                Line.of(111, 5),
                Line.of(5, 6),
                Line.of(111, 44),
                Line.of(2, 35),
                Line.of(2, 49),
                Line.of(222, 666),
                Line.of(888, 333),
                Line.of(333, 555)
        );
        Tree<Integer> tree = Tree.ofLines(lines).unwrap();
        List<Integer> treeStreamList = tree.stream()
                .map(Tree.Node::data)
                .distinct().sorted().toList();
        List<Integer> lineStreamList = lines.stream()
                .flatMap(Line::stream).distinct().sorted().toList();
        Assertions.assertEquals(treeStreamList.size(), lineStreamList.size());
        Assertions.assertEquals(treeStreamList, lineStreamList);
    }

    @Test
    public void test15() {
        List<Line<Integer>> lines = List.of(
                Line.of(111, 2),
                Line.of(2, 3),
                Line.of(3, 4),
                Line.of(111, 5),
                Line.of(5, 6),
                Line.of(111, 44),
                Line.of(2, 35),
                Line.of(2, 49),
                Line.of(222, 666),
                Line.of(888, 333),
                Line.of(333, 555)
        );
        Tree<Integer> tree = Tree.ofLines(lines).unwrap();
        List<Integer> lIterForEach = new ArrayList<>();
        List<Integer> lBFS = new ArrayList<>();
        StringJoiner sbIterForEach = new StringJoiner(", ");
        StringJoiner sbBFS = new StringJoiner(", ");
        for (Tree.Node<Integer> i : tree) {
            lIterForEach.add(i.data());
            sbIterForEach.add(i.data().toString());
        }
        tree.forEachBfs(n -> {
            lBFS.add(n.data());
            sbBFS.add(n.data().toString());
        });
        Assertions.assertEquals(lIterForEach, lBFS);
        Assertions.assertEquals(sbIterForEach.toString(), sbBFS.toString());
//        System.out.println(tree.displayString());
//        System.out.println(lIterForEach);
//        System.out.println(lBFS);
    }

    @Test
    public void test16() {
        List<Line<Integer>> lines = List.of(
                Line.of(111, 2),
                Line.of(2, 3),
                Line.of(3, 4),
                Line.of(111, 5),
                Line.of(5, 6),
                Line.of(111, 44),
                Line.of(2, 35),
                Line.of(2, 49),
                Line.of(222, 666),
                Line.of(888, 333),
                Line.of(333, 555)
        );
        Tree<Integer> tree = Tree.ofLines(lines).unwrap();
        Tree<Integer> emptyTree = Tree.empty();
        //System.out.println(Stf.f("tree: nodeCount: {}, depth: {}", tree.nodeCount(), tree.depth()));
        //System.out.println(tree.displayString());

        int callIterCount = 0;
        List<Tree.Node<Integer>> deletedNodeRefList = new ArrayList<>();
        List<Integer> iterPeeked = new ArrayList<>();
        while (!tree.isEmpty()) {
            if (callIterCount > 99) break;
            deletedNodeRefList.clear();
            iterPeeked.clear();
            callIterCount += 1;
            Iterator<Tree.Node<Integer>> iter = tree.iterator();
            int befCount = tree.nodeCount();
            while (iter.hasNext()) {
                // fix tree nodeCount and depth modify
                Tree.Node<Integer> n = iter.next();
                iterPeeked.add(n.data());
                long num = Long.parseLong(Rng.nextFixLenLarge(1));
                long test = num % 2;
                boolean randomDelete = test == 0;
                if (randomDelete) {
//                    System.out.println(Stf
//                            .f("RandomBoolean({}/2==0) is {}，currNodeData: {}, add to deletedList",
//                                    num, test, n.data()));
                    deletedNodeRefList.add(n);
                    iter.remove();
                }
            }
            // to do 生成大随机树，并测试
            int aftCount = tree.nodeCount();
            String peek = iterPeeked.stream().map(Object::toString).collect(Collectors.joining(" -> "));
//            System.out.println(Stf.f("callIterCount: {}, result: ", ++callIterCount));
//            System.out.println(Stf.f("peek: {}", peek));
//            System.out.println(Stf.f("直接删除的个数: {}", deletedNodeRefList.size()));
//            System.out.println(Stf.f("tree: nodeCount: {}, depth: {}", tree.nodeCount(), tree.depth()));
//            System.out.println(tree.displayString());
            Assertions.assertTrue(befCount >= aftCount);
//            System.out.println(Stf
//                    .f("IterPeeked size: {}, beforeCount: {}, afterCount: {}",
//                            iterPeeked.size(), befCount, aftCount));
        }
        Assertions.assertEquals(emptyTree.toDisplayStr(), tree.toDisplayStr());
    }

    @Test
    public void test17() {
        List<Line<Integer>> lines = List.of(
                Line.of(9, 12),
                Line.of(9, 15),
                Line.of(12, 10),
                Line.of(12, 6),
                Line.of(15, 6),
                Line.of(15, 8),
                Line.of(10, 2),
                Line.of(10, 18),
                Line.of(6, 18),
                Line.of(6, 1),
                Line.of(8, 1),
                Line.of(8, 5),
                Line.of(2, 19),
                Line.of(2, 7),
                Line.of(18, 7),
                Line.of(18, 11),
                Line.of(1, 11),
                Line.of(1, 4),
                Line.of(5, 4),
                Line.of(5, 16)
        );
        // 递归从根到某子路径，节点值之和最大的
        Map<Integer, List<Integer>> collect = lines.stream()
                .collect(Collectors.groupingBy(Line::begin,
                        Collectors.mapping(Line::end, Collectors.toList())));
        int maxCount = 0;
        for (Integer parentInt : collect.keySet()) {
            maxCount = Math.max(dfsCount(collect, parentInt), maxCount);
        }
        //System.out.println(maxCount);
        Assertions.assertEquals(60, maxCount);

    }

    // 递归从根到某子路径，节点值之和最大的
    public int dfsCount(Map<Integer, List<Integer>> collect, Integer intValue) {
        List<Integer> childInts = collect.get(intValue);
        if (childInts == null || childInts.isEmpty()) {
            return intValue;
        }
        List<Integer> dataCounts = new ArrayList<>(childInts.size());
        for (Integer child : childInts) {
            dataCounts.add(dfsCount(collect, child) + intValue);
        }
        Optional<Integer> max = dataCounts.stream().max(Integer::compareTo);
        return max.orElseThrow(RuntimeException::new);
    }

    @SuppressWarnings("UnnecessaryContinue")
    @Test
    public void test18() {
        // test17问题的动态规划解法
        List<Line<Integer>> lines = List.of(
                Line.of(9, 12),
                Line.of(9, 15),
                Line.of(12, 10),
                Line.of(12, 6),
                Line.of(15, 6),
                Line.of(15, 8),
                Line.of(10, 2),
                Line.of(10, 18),
                Line.of(6, 18),
                Line.of(6, 1),
                Line.of(8, 1),
                Line.of(8, 5),
                Line.of(2, 19),
                Line.of(2, 7),
                Line.of(18, 7),
                Line.of(18, 11),
                Line.of(1, 11),
                Line.of(1, 4),
                Line.of(5, 4),
                Line.of(5, 16)
        );
        Map<Integer, List<Integer>> collect = lines.stream()
                .collect(Collectors.groupingBy(Line::begin,
                        Collectors.mapping(Line::end, Collectors.toList())));
        int depth = 0;
        int thisDepthOffset = 0;
        int thisDepthMaxOffset = 0;
        int[][] lookupTable = new int[5][5];
        Queue<Integer> queue = new LinkedList<>();
        queue.add(9);
        //Set<Integer> isVisited = new HashSet<>();
        while (!queue.isEmpty()) {

            Integer current = queue.poll();
            if (depth == 0) {
                lookupTable[depth][thisDepthOffset] = current;
            } else {
                // 不是最上层，则其有上层
                if (thisDepthOffset == 0) {
                    // 最左边，没有左边上层
                    lookupTable[depth][thisDepthOffset] = current + lookupTable[depth - 1][thisDepthOffset];
                } else if (thisDepthOffset == thisDepthMaxOffset) {
                    // 最右边，没有右边上层
                    lookupTable[depth][thisDepthOffset] = current + lookupTable[depth - 1][thisDepthOffset - 1];
                } else {
                    // 左右上层都有的，比较
                    lookupTable[depth][thisDepthOffset] =
                            current + Math.max(lookupTable[depth - 1][thisDepthOffset - 1], lookupTable[depth - 1][thisDepthOffset]);
                }
            }

            // 将下一层元素推入队列中(如果有）
            List<Integer> next2QueueNullable = collect.get(current);
            if (next2QueueNullable != null) {
                for (Integer nextInt : next2QueueNullable) {
                    if (queue.contains(nextInt)) {
                        continue;
                    } else {
                        queue.add(nextInt);
                    }
                }
            }
            // 若已经到这层最后一个，则更新该层最大偏移量为下一层的最大，当前偏移量更新为0
            if (thisDepthOffset == thisDepthMaxOffset) {
                thisDepthMaxOffset += 1;
                thisDepthOffset = 0;
                depth += 1; // 层数向下
            } else {
                thisDepthOffset += 1;
            }
            // 队列中没元素，出来
        }
        int[] lastDepthNodeCountValues = lookupTable[depth - 1]; // while出来前把depth+1了，所以这里减一
        // 或者在while内把depth+1前判断当前是否有子，无子则不加1，这里就不用减一了
        // 找最大的，就是一路选下来的最优解
        OptionalInt max = IntStream.of(lastDepthNodeCountValues)
                .max();
        //System.out.println(max.orElseThrow(RuntimeException::new));
        Assertions.assertEquals(60, max.orElseThrow(RuntimeException::new));
    }

    @Test
    public void test19() {
        List<Line<Integer>> lines = List.of(
                Line.of(1, 2),
                Line.of(2, 3),
                Line.of(3, 4),
                Line.of(1, 5),
                Line.of(5, 6)
        );
        Tree<Integer> tree = Tree.ofLines(lines).unwrap();
//        System.out.println(tree.displayString());
        Iterator<Integer> it1 = tree.nodeDataIterator();
        Assertions.assertThrows(UnsupportedOperationException.class, it1::remove);
        Iterator<Tree.Node<Integer>> tIt1 = tree.iterator();
        Iterator<Tree.Node<Integer>> tIt2 = tree.iterator();
        tIt1.next();
        tIt2.next();
        tIt1.remove();
        Assertions.assertThrows(ConcurrentModificationException.class, tIt2::next);

    }

    @Test
    public void test20() {
        // no IdHash eq roots test
        NoEqAndHash<Integer> n1 = NoEqAndHash.ofValue(1);
        NoEqAndHash<Integer> n2 = n1;
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Tree.ofRoots(List.of(n1, n2), (e) -> null);
        });

    }

    @Test
    public void test21() {
        NoEqAndHash<Integer> n1 = NoEqAndHash.ofValue(1);
        NoEqAndHash<Integer> n2 = NoEqAndHash.ofValue(2);
        NoEqAndHash<Integer> n3 = NoEqAndHash.ofValue(3);
        Map<NoEqAndHash<Integer>, List<NoEqAndHash<Integer>>> getChild = Map.of(
                n1, List.of(n2),
                n2, List.of(n3),
                n3, List.of(n1)
        );
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Tree.ofRoots(List.of(n1), (e) -> getChild.get(e));
        });

        String sA = new String("a");
        String sB = new String("b");
        String sC = new String("c");
        String sD = new String("d");
        String sE = new String("e");
        Map<String, List<String>> mapGetChild = Map.of(
                sA, List.of(sB),
                sB, List.of(sC),
                sC, List.of(sD),
                sD, List.of(sE),
                sE, List.of(sA)
        );
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Tree.ofRoots(List.of(sA), (e) -> mapGetChild.get(e));
        });

        Map<String, List<String>> mapGetChild2 = Map.of(
                sA, List.of(sB),
                sB, List.of(sC),
                sC, List.of(sD, sE),
                sD, List.of(sE)
        );
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Tree.ofRoots(List.of(sA), (e) -> mapGetChild2.get(e));
        });

    }

    @Test
    public void test22() {
        // overwrite eq and hash obj
        String sA = new String("a");
        String sB = new String("b");
        String sC = new String("c");
        String sD = new String("d");
        String sE = new String("e");
        Map<String, List<String>> mapGetChild = Map.of(
                sA, List.of(sB),
                sB, List.of(sC),
                sC, List.of(sD),
                sD, List.of(sE)
        );
        var tree = Tree.ofRoots(List.of(sA), (e) -> mapGetChild.get(e));
        //System.out.println(tree.displayString());
        Map<String, List<String>> mapGetChild2 = Map.of(
                "a", List.of("b"),
                "b", List.of("c"),
                "c", List.of("d"),
                "d", List.of("e")
        );
        var tree2 = Tree.ofRoots(List.of(sA), (e) -> mapGetChild.get(e));
        //System.out.println(tree2.displayString());
        // ===================================
        // no overwrite eq and hash obj
        NoEqAndHash<Integer> n1 = NoEqAndHash.ofValue(1);
        Map<NoEqAndHash<Integer>, List<NoEqAndHash<Integer>>> getChild = Map.of(
                NoEqAndHash.ofValue(1), List.of(NoEqAndHash.ofValue(2)),
                NoEqAndHash.ofValue(2), List.of(NoEqAndHash.ofValue(3))
        );
        Assertions.assertDoesNotThrow(() -> {
            var tree3 = Tree.ofRoots(List.of(n1), e -> getChild.get(e));
            //System.out.println(tree3.displayString());
        });
    }

    @Test
    public void test23() {
        // use ofNodes test
        Map<String, List<String>> sGetChild = Map.of(
                "a", List.of("b"),
                "b", List.of("c"),
                "c", List.of("d"),
                "d", List.of("e")
        );
        var tree1 = Tree.ofRoots(List.of("a"), (e) -> sGetChild.get(e));
        Tree.Node<String> root = tree1.root().get(0);
        Tree.Node<String> nc = null;
        Iterator<Tree.Node<String>> iter = tree1.iterator();
        while (iter.hasNext()) {
            var n = iter.next();
            if (n.data().equals("c")) {
                nc = n;
                break;
            }
        }
        Assertions.assertNotNull(nc);
        Tree.Node<String> finalNc = nc; // lambda final
        Assertions.assertThrows(R.UnwrapException.class, () -> {
            Tree<String> tree2 = Tree.ofNodes(List.of(root, finalNc)).unwrap();
        });

        Assertions.assertDoesNotThrow(() -> {
            Tree<String> tree3 = Tree.ofNodes(List.of(finalNc)).unwrap();
            //System.out.println(tree3.displayString());
            //System.out.println(tree3);
        });


    }


    private Tup2<List<Line<Integer>>, Tree<Integer>> genBigTree(int nodeCountOrigin,
                                                                int nodeCountBound,
                                                                int rootCountOrigin,
                                                                int rootCountBound) {

        // 生成一个大的随机树，节点数在1000-2000之间
        int nodeCount = (int) Rng.nextLong(nodeCountOrigin, nodeCountBound);

        // 生成节点值列表，确保值唯一
        Set<Integer> values = new HashSet<>();
        while (values.size() < nodeCount) {
            values.add((int) Rng.nextLong(1, Integer.MAX_VALUE));
        }
        List<Integer> nodeValues = new ArrayList<>(values);

        // 随机选择1-5个根节点
        int rootCount = (int) Rng.nextLong(rootCountOrigin, rootCountBound);
        List<Integer> roots = new ArrayList<>();
        for (int i = 0; i < rootCount; i++) {
            roots.add(nodeValues.remove(0));
        }
        // 构建边的关系
        List<Line<Integer>> lines = new ArrayList<>();
        List<Integer> availableParents = new ArrayList<>(roots);

        while (!nodeValues.isEmpty()) {
            // 随机选择一个父节点
            int parentIndex = (int) Rng.nextLong(availableParents.size());
            int parent = availableParents.get(parentIndex);

            // 随机决定当前父节点要添加多少个子节点(1-5个)
            int childCount = (int) Rng.nextLong(1, Math.min(6, nodeValues.size() + 1));

            for (int i = 0; i < childCount && !nodeValues.isEmpty(); i++) {
                int child = nodeValues.remove(0);
                lines.add(Line.of(parent, child));
                availableParents.add(child);
            }
        }
        // 构建树
        Tree<Integer> tree = Tree.ofLines(lines).unwrap();
        return Tup2.of(lines, tree);
    }


    @Test
    public void testGenerateLargeRandomTree() {

        int nodeCountOrigin = 1000;
        int nodeCountBound = 2000;
        int rootCountOrigin = 1;
        int rootCountBound = 5;
        Tup2<List<Line<Integer>>, Tree<Integer>> lineAndTree = genBigTree(
                nodeCountOrigin, nodeCountBound, rootCountOrigin, rootCountBound);
        List<Line<Integer>> lines = lineAndTree.l();
        Tree<Integer> tree = lineAndTree.r();
//        System.out.println(tree);
//        System.out.println(tree.displayString());

        // 验证树的属性
        Assertions.assertTrue(tree.nodeCount() >= nodeCountOrigin);
        Assertions.assertTrue(tree.nodeCount() <= nodeCountBound);

        // 验证树的结构
        // 1. 将树转换回边的关系
        List<Line<Integer>> convertedLines = tree.toLines(ArrayList::new, Tree.Node::data).unwrap();

        // 2. 排序后比较原始边和转换后的边是否相等
        Comparator<Line<Integer>> lineComparator = Comparator
                .<Line<Integer>, Integer>comparing(Line::begin)
                .thenComparing(Line::end);

        List<Line<Integer>> sortedOriginalLines = lines.stream()
                .sorted(lineComparator)
                .toList();
        List<Line<Integer>> sortedConvertedLines = convertedLines.stream()
                .sorted(lineComparator)
                .toList();

        Assertions.assertEquals(sortedOriginalLines, sortedConvertedLines);

        // 3. 测试树的遍历
        Set<Integer> bfsNodes = new HashSet<>();
        tree.forEachBfs(node -> bfsNodes.add(node.data()));
        Assertions.assertEquals(tree.nodeCount(), bfsNodes.size());

        // 4. 测试迭代器
        Set<Integer> iteratorNodes = new HashSet<>();
        for (Tree.Node<Integer> value : tree) {
            iteratorNodes.add(value.data());
        }
        Assertions.assertEquals(tree.nodeCount(), iteratorNodes.size());
        Assertions.assertEquals(bfsNodes, iteratorNodes);

        // 5. 测试节点删除
        Iterator<Tree.Node<Integer>> nodeIterator = tree.iterator();
        int initialCount = tree.nodeCount();
        int deletedCount = 0;

        while (nodeIterator.hasNext()) {
            Tree.Node<Integer> node = nodeIterator.next();
            // 随机删除节点(50%概率)
            if (Rng.nextBoolean()) {
                int beforeDelete = tree.nodeCount();
                nodeIterator.remove();
                int afterDelete = tree.nodeCount();
                int delta = beforeDelete - afterDelete;
                Assertions.assertTrue(delta > 0);
                deletedCount += delta;
            }
        }

        Assertions.assertEquals(initialCount - tree.nodeCount(), deletedCount);
    }

    @Test
    public void testLargeTreeProcess() {

        int nodeCountOrigin = 1000;
        int nodeCountBound = 3000;
        int rootCountOrigin = 10;
        int rootCountBound = 20;
        Tup2<List<Line<Integer>>, Tree<Integer>> listTreeTup2 = genBigTree(
                nodeCountOrigin, nodeCountBound, rootCountOrigin, rootCountBound);
        Tree<Integer> tree = listTreeTup2.r();
        List<Tree<Integer>> spTree = tree.split();
        Tree<Integer> tree1 = Tree.ofNodes(spTree.stream()
                .map(Tree::root)
                .flatMap(List::stream)
                .toList()).unwrap();
        //System.out.println(Stf.f("tree: {}", tree));
        //spTree.forEach(t -> System.out.println(Stf.f("sp_tree: {}", t)));
        //System.out.println(Stf.f("tree1: {}", tree1));
        Assertions.assertEquals(tree1.nodeCount(), tree.nodeCount());
        Assertions.assertEquals(tree1.depth(), tree.depth());

        List<Integer> t1Collector = new ArrayList<>();
        List<Integer> t2Collector = new ArrayList<>();
        tree.forEach(t -> t1Collector.add(t.data()));
        tree1.forEach(t -> t2Collector.add(t.data()));
        Assertions.assertEquals(t1Collector.size(), t2Collector.size());
        Collections.sort(t1Collector);
        Collections.sort(t2Collector);
        Assertions.assertEquals(t1Collector, t2Collector);

        // 排序
        tree.sort(Integer::compareTo);
        tree1.sort(Integer::compareTo);

        // try 一个一个比较
        int nCount = tree.nodeCount();
        int idx = 0;
        Iterator<Integer> tIt = tree.nodeDataIterator();
        Iterator<Integer> t1It = tree1.nodeDataIterator();
        while (++idx <= nCount) {
            Integer next = tIt.next();
            Integer next1 = t1It.next();
            Assertions.assertEquals(next, next1);
        }


    }

    @SuppressWarnings("unused")
    @Test
    public void testToJson() {

        Map<String, List<String>> sGetChild = Map.of(
                "a", List.of("b"),
                "b", List.of("c"),
                "c", List.of("d"),
                "d", List.of("e")
        );
        var tree1 = Tree.ofRoots(List.of("a"), (e) -> sGetChild.get(e));
        String sub = tree1.toJsonStr(10, "node", "child",
                n -> Stf.f("{\"depth\": {}, \"data\": \"{}\"}",
                        n.depth(), n.data()));
        //System.out.println(sub);

    }


    @Test
    public void testToJson2() {
        record Obj(int id, String name) {
            static Obj ofId(int id) {
                return new Obj(id, String.valueOf(id));
            }
        }
        List<Obj> objs = List.of(
                Obj.ofId(0), Obj.ofId(1), Obj.ofId(2),
                Obj.ofId(3), Obj.ofId(4), Obj.ofId(5)
        );
        Map<Obj, List<Obj>> getChild = Map.of(
                objs.get(0), List.of(objs.get(1)),
                objs.get(1), List.of(objs.get(2), objs.get(3)),
                objs.get(3), List.of(objs.get(4)),
                objs.get(4), List.of(objs.get(5))
        );
        Tree<Obj> treeObj = Tree.ofRoots(List.of(objs.get(0)),
                getChild::get,
                Comparator.comparingInt(Obj::id),
                getChild::containsKey,
                Predicate.isEqual(null).negate(),
                Integer.MAX_VALUE);
        String displayStr = treeObj.toDisplayStr();
        //System.out.println(displayStr);


        ObjectMapper mapper = new ObjectMapper();
        String jsonStr = treeObj.toJsonStr(2, "n", "c",
                n -> {
                    try {
                        return mapper.writeValueAsString(n.data());
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
        //System.out.println(jsonStr);

        Tree<Obj2> tree = Tree.ofLines(
                List.of(
                        Line.of(
                                new Obj2(1, "a"),
                                new Obj2(2, "b")
                        )
                )
        ).unwrap();
        String json = tree.toJsonStr(tree.depth(),
                "obj",
                "child",
                node -> {
                    try {
                        return mapper.writeValueAsString(node.data());
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
        //System.out.println(json);

        Tree<Obj2> emptyTree = Tree.empty();
        String emptyTreeJson = emptyTree.toJsonStr(Integer.MAX_VALUE,
                "objSelf",
                "objChild",
                node -> {
                    try {
                        return mapper.writeValueAsString(node.data());
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
        Assertions.assertEquals("[]", emptyTreeJson);


    }

    record Obj2(int id, String name) {
    }

    @Test
    public void test25() {
        R<Tree<Object>> treeR = Tree.ofNodes(List.of());
        Assertions.assertDoesNotThrow(() -> {
            Tree<Object> tr = treeR.unwrap();
            Assertions.assertTrue(tr.isEmpty());
        });
    }

    @Test
    public void test26() {
        Assertions.assertThrows(R.UnwrapException.class, () -> {
            R<Tree<Object>> treeR = Tree.ofNodes(null);
            treeR.unwrap();
        });
        Assertions.assertDoesNotThrow(() -> {
            R<Tree<Object>> treeR = Tree.ofNodes(List.of());
            treeR.unwrap();
        });
    }

    @Test
    public void test27() {
        // chopTest
        Tree<Integer> tree = genBigTree(1000, 2000, 1, 10).r();
        //System.out.println(tree.toDisplayStr());
        //System.out.println(tree);
        int treeDepth = tree.depth();
        int nodeCount = tree.nodeCount();
        while (treeDepth-- > -1) {
            //System.out.println("==============");
            nodeCount -= tree.rootCount();
            int befNodeCount = tree.nodeCount();
            int befDepth = tree.depth();

            List<Tree.Node<Integer>> befRoots = tree.root();
            tree.chopRoot();
            //System.out.println(tree.toDisplayStr());
            int afterDepth = tree.depth();
            int afterNodeCount = tree.nodeCount();
            //System.out.println(Stf
            //        .f("tree:{}, lNodeCount:{}, befNC:{}, aftNC:{}, befDepth:{}, aftDepth:{}",
            //                tree, nodeCount, befNodeCount, afterNodeCount, befDepth, afterDepth));
            Assertions.assertEquals(nodeCount, afterNodeCount);
            Assertions.assertEquals(befDepth - 1, afterDepth);
            int newRootCount = tree.rootCount();
            if (newRootCount > 0) {
                int idx = Rng.nextInt(0, newRootCount);
                Tree.Node<Integer> rngRoot = tree.root(idx);
                Assertions.assertTrue(rngRoot.isRoot());
                Assertions.assertEquals(0, rngRoot.depth());
            }
            for (Tree.Node<Integer> befRoot : befRoots) {
                Assertions.assertTrue(befRoot.isPruned());
            }

        }
        Assertions.assertEquals(-1, tree.depth());
        Assertions.assertTrue(tree.isEmpty());

    }

    @Test
    public void test28() {
        Tree<Integer> tree = genBigTree(1000, 2000, 1, 10).r();
        Iterator<Tree.Node<Integer>> iter = tree.iterator();
        Tree<Integer> self = tree.pruneRoot(0);
        Assertions.assertSame(tree, self);
        Assertions.assertThrows(ConcurrentModificationException.class, iter::hasNext);
        Assertions.assertThrows(ConcurrentModificationException.class, iter::next);
        Assertions.assertThrows(ConcurrentModificationException.class, iter::remove);
        Iterator<Tree.Node<Integer>> itAfter = self.iterator();
        Assertions.assertThrows(IllegalStateException.class, itAfter::remove);
        Assertions.assertDoesNotThrow(itAfter::next);
        Assertions.assertDoesNotThrow(itAfter::remove);
    }

    @Test
    public void test29() {
        Tree<Integer> tree = genBigTree(1000, 2000, 1, 10).r();
        Tree.Node<Integer> root = tree.root(Rng.nextInt(0, tree.rootCount()));
        Tree.Node<Integer> anyNotRootNode = null;
        int befNodeCount = tree.nodeCount();
        int befRtCount = tree.rootCount();
        boolean doPrune = false;
        if (!root.isLeaf()){
            for (Tree.Node<Integer> cnode : root.childNode()) {
                Assertions.assertFalse(cnode.isPruned());
            }
            int childCount = root.childCount();
            int i = Rng.nextInt(0, childCount);
            anyNotRootNode = root.childNode().get(i);
            root.mut().prune();
            doPrune = true;
        }
        if (anyNotRootNode != null) {
            Assertions.assertTrue(anyNotRootNode.isPruned());
        }
        if (doPrune) {
            Assertions.assertNotEquals(befNodeCount, tree.nodeCount());
            Assertions.assertNotEquals(befRtCount, tree.rootCount());
        }
    }


}
