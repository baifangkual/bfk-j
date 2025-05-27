package io.github.baifangkual.bfk.j.mod.core.lang;

import io.github.baifangkual.bfk.j.mod.core.util.Stf;
import io.github.baifangkual.bfk.j.mod.core.util.Rng;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
        Tree<Integer> tree = Tree.tryOfLines(lines).unwrap();
        String beforeIter = tree.displayString();
        Iterator<Tree.Node<Integer>> it = tree.nodeIterator();
        while (it.hasNext()) {
            Tree.Node<Integer> n = it.next();
            if (n == null) throw new IllegalStateException(); // do nothing...
        }
        String afterIter = tree.displayString();
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
        Tree<Integer> tree = Tree.tryOfLines(lines).unwrap();
        String beforeIter = tree.displayString();
        Iterator<Tree.Node<Integer>> it = tree.nodeIterator();
        while (it.hasNext()) {
            Tree.Node<Integer> n = it.next();
            if (n.isLeaf()) {
                it.remove(); // will remove 4 and 6
            }
        }
        String afterIter = tree.displayString();
        List<Line<Integer>> newNo4And6EndList = lines.stream()
                .filter(l -> (!l.end().equals(4)) && (!l.end().equals(6)))
                .toList();
        Tree<Integer> newTree = Tree.tryOfLines(newNo4And6EndList).unwrap();
        String newIter = newTree.displayString();
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
        Tree<Integer> tree = Tree.tryOfLines(lines).unwrap();
        String beforeIter = tree.displayString();
        Iterator<Tree.Node<Integer>> it = tree.nodeIterator();
        while (it.hasNext()) {
            Tree.Node<Integer> n = it.next();
            if (n.data().equals(2)) {
                it.remove(); // will 2 and childNodes: 3,4
            }
        }
        String afterIter = tree.displayString();
        List<Line<Integer>> newNo234List = List.of(
                Line.of(1, 5),
                Line.of(5, 6)
        );
        Tree<Integer> newTree = Tree.tryOfLines(newNo234List).unwrap();
        String newIter = newTree.displayString();
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
        Tree<Integer> tree = Tree.tryOfLines(lines).unwrap();
        Tree<Integer> emptyTree = Tree.empty();
        Iterator<Tree.Node<Integer>> iter = tree.nodeIterator();
        while (iter.hasNext()) {
            // fix tree nodeCount and depth modify
            iter.next();
            iter.remove();
        }
        Assertions.assertEquals(emptyTree.displayString(), tree.displayString());
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
        Tree<Integer> tree = Tree.tryOfLines(lines).unwrap();
        Tree<Integer> emptyTree = Tree.empty();
        Iterator<Tree.Node<Integer>> iter = tree.nodeIterator();
        //System.out.println(tree.displayString());
        while (iter.hasNext()) {
            // fix tree nodeCount and depth modify
            Tree.Node<Integer> n = iter.next();
            //System.out.println(n);
            iter.remove();
        }
        Assertions.assertEquals(emptyTree.displayString(), tree.displayString());
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
        Tree<Integer> tree = Tree.tryOfLines(lines).unwrap();
        Tree<Integer> emptyTree = Tree.empty();
//        System.out.println(STF.f("tree: nodeCount: {}, depth: {}", tree.nodeCount(), tree.depth()));
//        System.out.println(tree.displayString());
        int callIterCount = 0;
        List<Integer> deleted = new ArrayList<>();
        List<Integer> iterPeeked = new ArrayList<>();
        while (!tree.isEmpty()) {
            deleted.clear();
            iterPeeked.clear();
            Iterator<Tree.Node<Integer>> iter = tree.nodeIterator();
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
        Assertions.assertEquals(emptyTree.displayString(), tree.displayString());
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
        Tree<Integer> tree = Tree.tryOfLines(lines).unwrap();
        List<Integer> treeStreamList = tree.stream()
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
        Tree<Integer> tree = Tree.tryOfLines(lines).unwrap();
        List<Integer> lIterForEach = new ArrayList<>();
        List<Integer> lBFS = new ArrayList<>();
        StringJoiner sbIterForEach = new StringJoiner(", ");
        StringJoiner sbBFS = new StringJoiner(", ");
        for (Integer i : tree) {
            lIterForEach.add(i);
            sbIterForEach.add(i.toString());
        }
        tree.bfs(n -> {
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
        Tree<Integer> tree = Tree.tryOfLines(lines).unwrap();
        Tree<Integer> emptyTree = Tree.empty();
        System.out.println(Stf.f("tree: nodeCount: {}, depth: {}", tree.nodeCount(), tree.depth()));
        System.out.println(tree.displayString());

        int callIterCount = 0;
        List<Tree.Node<Integer>> deletedNodeRefList = new ArrayList<>();
        List<Integer> iterPeeked = new ArrayList<>();
        while (!tree.isEmpty()) {
            if (callIterCount > 99) break;
            deletedNodeRefList.clear();
            iterPeeked.clear();
            Iterator<Tree.Node<Integer>> iter = tree.nodeIterator();
            int befCount = tree.nodeCount();
            while (iter.hasNext()) {
                // fix tree nodeCount and depth modify
                Tree.Node<Integer> n = iter.next();
                iterPeeked.add(n.data());
                long num = Long.parseLong(Rng.rollFixLenLarge(1));
                long test = num % 2;
                boolean randomDelete = test == 0;
                if (randomDelete) {
                    System.out.println(Stf
                            .f("RandomBoolean({}/2==0) is {}，currNodeData: {}, add to deletedList",
                                    num, test, n.data()));
                    deletedNodeRefList.add(n);
                    iter.remove();
                }
            }
            // todo 生成大随机树，并测试
            int aftCount = tree.nodeCount();
            String peek = iterPeeked.stream().map(Object::toString).collect(Collectors.joining(" -> "));
            System.out.println(Stf.f("callIterCount: {}, result: ", ++callIterCount));
            System.out.println(Stf.f("peek: {}", peek));
            System.out.println(Stf.f("直接删除的个数: {}", deletedNodeRefList.size()));
            System.out.println(Stf.f("tree: nodeCount: {}, depth: {}", tree.nodeCount(), tree.depth()));
            System.out.println(tree.displayString());
            Assertions.assertTrue(befCount >= aftCount);
            System.out.println(Stf
                    .f("IterPeeked size: {}, beforeCount: {}, afterCount: {}",
                            iterPeeked.size(), befCount, aftCount));
        }
        Assertions.assertEquals(emptyTree.displayString(), tree.displayString());
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
            if (next2QueueNullable != null){
                for (Integer nextInt : next2QueueNullable) {
                    if (queue.contains(nextInt)) {continue;} else {queue.add(nextInt);}
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
        Tree<Integer> tree = Tree.tryOfLines(lines).unwrap();
//        System.out.println(tree.displayString());
        Iterator<Integer> it1 = tree.iterator();
        Iterator<Integer> it2 = tree.iterator();
        it1.next();
        it2.next();
        it1.remove();
        Assertions.assertThrows(ConcurrentModificationException.class, it2::remove);

    }


}
