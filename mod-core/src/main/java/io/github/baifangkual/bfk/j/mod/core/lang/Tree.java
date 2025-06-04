package io.github.baifangkual.bfk.j.mod.core.lang;


import io.github.baifangkual.bfk.j.mod.core.Const;
import io.github.baifangkual.bfk.j.mod.core.mark.Iter;
import io.github.baifangkual.bfk.j.mod.core.panic.Err;
import io.github.baifangkual.bfk.j.mod.core.util.Stf;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <b>Tree</b>
 * <p>多叉树，通用树，状态可变，线程不安全，可以存在多个根节点
 * <p>该树的深度 {@link #depth} 使用边数计算法（空树（{@link #root} is empty）的深度 = -1，只有根节点的树的深度 = 0）
 * <p>该树可被迭代，可通过迭代器迭代树中所有元素，返回的迭代器的行为可查看 {@link Tree#iterator()} 说明<br>
 * 该树存在的原因是对一批有逻辑从属关系且能构成树型结构的实体进行方便操作和部分特征的查询，换言之，该树仅应当为一种临时数据结构，
 * 不建议以任何形式序列化该树（因为树通常很大，即使java原生的序列化行为可以正确处理循环引用），
 * 强行的序列化可能造成 {@code StackOverflowError} 异常（因为树深度过深且可能含有循环引用），
 * 同样基于该原因，该类型也不应实现 {@code equals & hashcode} 方法
 * <p>因为该树为二叉树的父集，遂没有中序遍历
 * <p>该树中，某节点 {@code Tree.Node} 的“删除（剪枝）”语义是：将自身及所有直接和间接子节点从 {@code Tree} 中删除，
 * 被删除的 {@code Tree.Node} 从树中一定不会访问到，其 {@code node.isPruned() == true}，
 * 基于上述 {@code Tree.Node} 被变更时其自身的状态信息的变化的原因，
 * 外界不应当长期持有 {@code Tree.Node} 的引用，而是应当持有 {@code Tree} 的引用<br>
 *
 * @author baifangkual
 * @apiNote 构建树时会使用需存储在树中元素 {@link T} 的 {@code IdentityHash} 来判定树中元素是否有重复，
 * 以此来检查是否有 循环边/循环引用（环）和 是否存在入度大于1的节点，当这种非法情况发生，抛出异常，即说明无法构成树型结构，
 * 若能够构成树型结构，即一定一个树中的 {@link T} 没有重复 {@code IdentityHash}
 * @see #identityHash(Object)
 * @see #ofRoots(Iterable, Function, Comparator, Predicate, Predicate, int)
 * @see #ofRoots(Iterable, Function)
 * @see #ofLines(Iterable)
 * @see #nodeDataIterator()
 * @see Tree#iterator()
 * @see #forEachBfs(Consumer)
 * @see #forEachDfsPreOrder(Consumer)
 * @see #forEachDfsPostOrder(Consumer)
 * @see #toDisplayStr()
 * @see #toJsonStr(int, String, String, Function)
 * @since 2023/8/18 v0.0.6
 */
public final class Tree<T> implements Iter<Tree.Node<T>> {

    // ref emptyTree
    private static final Tree<?> EMPTY_TREE_IMMUTABLE = new Tree<>(); // emptyTree，immutable
    private static final Consumer<Object> FN_ACC_DO_NOTHING = (n) -> { /* do nothing... */};
    /**
     * 销毁/破坏/剪掉 node，将其对宿主树的引用置空
     */
    private static final Consumer<UnsafeNode<?>> FN_PRUNE_NODE = (n) -> {
        n.unsafeSetHost(null);
    };

    // immutable
    private final Supplier<? extends List<UnsafeNode<T>>> listFactory; // 各node中childNodes类型
    private final List<UnsafeNode<T>> root;


    // ------- mutable
    private int nodeCount = 0;
    private int depth = -1;
    // 用以验证是否有同时使用tree的临时视图（如迭代器）对树做修改，对树做完修改后，
    // 修改树的实体（Iter）需要同步该状态与树相同
    private int modifyCount = 0;
    // ------- mutable


    @SuppressWarnings("unchecked")
    public static <T> Tree<T> empty() {
        return (Tree<T>) EMPTY_TREE_IMMUTABLE;
    }

    public boolean isEmpty() {
        return nodeCount == 0;
    }

    /**
     * 返回所有根节点<br>
     * 返回的 List 的类型是 {@code unmodifiableList}
     *
     * @return 根节点
     */
    public List<Node<T>> root() {
        // fix: 调用时返回快照，该返回应对后续List<Root>本身的变动不可见
        return root.isEmpty() ? Collections.emptyList() :
                root.stream().map(un -> (Node<T>) un).toList();
    }

    /**
     * 返回指定位置的根节点<br>
     * 若指定位置没有节点，则返回 {@link Optional#empty()}
     *
     * @param idx 索引
     * @return Optional(rootNode) | Optional.Empty
     */
    public Optional<Node<T>> tryRoot(int idx) {
        return rootCount() <= idx ? Optional.empty() : Optional.of(root.get(idx));
    }

    /**
     * 返回指定位置的根节点
     *
     * @param idx 索引
     * @return 根节点
     * @throws IndexOutOfBoundsException 索引位置根节点不存在 ({@code index < 0 || index >= size()})
     */
    public Node<T> root(int idx) throws IndexOutOfBoundsException {
        return root.get(idx);
    }


    /**
     * 根节点个数
     *
     * @return 根节点个数
     */
    public int rootCount() {
        return root.size();
    }

    /**
     * 树深度
     *
     * @return 树深度
     */
    public int depth() {
        return depth;
    }

    /**
     * 树中实体个数
     *
     * @return 树中实体个数
     */
    public int nodeCount() {
        return nodeCount;
    }

    /**
     * 砍掉所有根节点，将所有根节点的直接子节点作为新的根节点
     * <p>如果树是空树，则什么都不做，否则，树的所有剩余节点的 {@code depth} 减一，
     * 可以想象的，如果持续进行该操作，树最终会成为空树
     * <p>该方法会修改树的节点状态</p>
     * <pre>{@code
     * Tree<T> tree = ...;
     * // tree before(root is 1, root.child is 2 & 5):
     * // │/
     * // │└─ 1
     * // │   ├─ 2
     * // │   │  └─ 3
     * // │   │     └─ 4
     * // │   └─ 5
     * // │      └─ 6
     * tree.chopRoot();
     * // tree after(root is 2 & 5):
     * // │/
     * // │└─ 2
     * // │   └─ 3
     * // │      └─ 4
     * // │/
     * // │└─ 5
     * // │   └─ 6
     * }</pre>
     *
     * @return this
     */
    public Tree<T> chopRoot() {
        if (isEmpty()) {
            return this;
        }
        int befRootCount = rootCount();
        List<UnsafeNode<T>> clearParentDepth1ChildNode = this.root.stream()
                .map(UnsafeNode::unsafeGetChildNode)
                .filter(Objects::nonNull)
                .filter(clist -> !clist.isEmpty())
                .flatMap(List::stream)
                // 清除对原root的引用，刚好使其表意为root的表意（parentNode isnull）
                .peek(nr -> nr.unsafeSetParentNode(null))
                .toList();
        // 清除对当前树的引用，使其 isPruned == true
        this.root.forEach(or -> or.unsafeSetHost(null));
        this.root.clear();
        this.root.addAll(clearParentDepth1ChildNode);
        // 更新节点的depth
        this.bfs(() -> root, n -> n.unsafeSetDepth(n.depth() - 1));
        // 更新树的depth 和 nodeCount，更新modCount
        this.nodeCount -= befRootCount;
        this.depth -= 1;
        incrementModifyCount();
        return this;
    }

    /**
     * 剪掉给定索引位置的根节点
     * <p>该方法会修改树的节点状态</p>
     *
     * @param idx 索引
     * @return this
     * @throws IndexOutOfBoundsException 索引位置根节点不存在 ({@code index < 0 || index >= size()})
     */
    public Tree<T> pruneRoot(int idx) {
        Node<T> r = tryRoot(idx).orElseThrow(() -> new IndexOutOfBoundsException(Stf
                .f("idx: {}, not found root, rootCount: {}",
                        idx, rootCount())));
        pruneNodeAndUpdateTreeState(r);
        return this;
    }

    @Override
    public Stream<Node<T>> stream() {
        return into(LinkedList::new).stream();
    }

    /**
     * 剪掉给定索引位置的根节点
     * <p>该方法可能会修改树的节点状态</p>
     *
     * @param idx 索引
     * @return this
     */
    public Tree<T> tryPruneRoot(int idx) {
        tryRoot(idx).ifPresent(this::pruneNodeAndUpdateTreeState);
        return this;
    }

    /**
     * 空树，不可变，不可添加节点
     */
    private Tree() {
        this(Collections.emptyList(),
                n -> null,
                Comparator.nullsFirst(null),
                Collections::emptyList,
                n -> false,
                n -> false,
                -1
        );
    }


    /**
     * 构造方法
     *
     * @param roots              根节点，一个或多个
     * @param fnGetChild         函数，<b>要求给定一个实体，返回这个实体的直接子实体，返回的可迭代对象可以为null，也可以没有元素，
     *                           在使用默认{@code maxDepth}参数的情况下，
     *                           该方法一定要有穷，即一定能够找到逻辑上的叶子节点，否则可能会造成栈内存溢出</b>
     * @param fnSort             函数-实体排序的函数<b>(该函数仅在Tree构造时使用，Tree构造完成便被丢弃）</b>
     * @param listFactory        函数-List构造方法引用（形如 {@code ArrayList::new}），
     *                           返回的List用以装载 {@code root} 和 {@code Node.childNode},
     *                           函数返回的List一定要可读可写，否则构造树时会抛出异常
     * @param fnPreNeedFindChild 函数(nullable)，要求给定一个实体，返回布尔值标识该实体是否需要寻找直接子实体，
     *                           该函数仅在对元素执行 {@code fnGetChild} 前执行，当该函数返回 {@code false}，
     *                           则 {@code fnGetChild} 函数一定不会对当前元素执行，否则，
     *                           函数 {@code fnGetChild} 一定会对当前元素执行，
     *                           这在 {@code fnGetChild} 过重时是一种优化<b>(该函数仅在Tree构造时使用）</b>
     * @param fnPreFilter        函数，预先处理树中实体，要求给定一个实体，当返回的布尔值为false时，
     *                           表示实体不加入树<b>(该函数仅在Tree构造时使用，Tree构造完成便被丢弃）</b>
     * @param maxDepth           <b>最大深度（包含），表示该树所承载的最大层级，
     *                           当已达到树所承载的最大深度时，即使树当前的叶子节点仍可通过 {@code fnGetChild} 获取子节点，
     *                           也不会将其添加到树中</b>
     * @throws NullPointerException     当不允许为空的参数给定空时
     * @throws IllegalArgumentException 当给定的最大停止深度小于 -1 时
     * @throws IllegalArgumentException 当构建树的过程发现循环边/循环引用/存在节点大于1个入度时
     */
    Tree(Iterable<? extends T> roots,
         Function<? super T, ? extends Iterable<T>> fnGetChild,
         Comparator<? super T> fnSort,
         Supplier<? extends List<UnsafeNode<T>>> listFactory,
         Predicate<? super T> fnPreNeedFindChild,
         Predicate<? super T> fnPreFilter,
         int maxDepth) {

        // null check （fnPreCheck is nullable, no need check）
        Objects.requireNonNull(fnSort, "fnSort is null");
        Objects.requireNonNull(roots, "roots is null");
        Objects.requireNonNull(listFactory, "listFactory is null");
        Objects.requireNonNull(fnGetChild, "fnGetChild is null");
        Objects.requireNonNull(fnPreFilter, "fnPreFilter is null");

        // given maxDepth < -1 Illegal
        Err.realIf(maxDepth < -1, IllegalArgumentException::new, "maxDepth < -1");

        this.listFactory = listFactory;

        // compose fnHasChild and fnGetChild
        final Function<? super T, ? extends Iterable<T>> fnCompGetChild =
                compGetChildFn(fnPreNeedFindChild, fnGetChild);
        // node comp
        final Comparator<Node<T>> fnNSort = compNSortFn(fnSort);
        // 需隔绝外界给定的roots的Iter
        // 临时List存储root，root通常不多
        List<UnsafeNode<T>> rootCollector = listFactory.get();

        // fix: 使用IdentityHashMap引用T，确保方法feGetChild不会直接或间接（多次后）又返回T自身，
        // 否则会出现递归一直无法结束，也不应当允许出现roots中相同的，否则会从树根构建两个完全相同的
        // 树，虽然仅roots有相同的不会造成循环引用，但这种roots相同的无法表达该树定义
        // ---- 似乎用普通hash也可以？
        Set<T> identityHashSet = Collections.newSetFromMap(new IdentityHashMap<>());

        // roots build
        Iter.toStream(roots)
                .filter(fnPreFilter)
                .peek(identityHashSet::add)
                .map(t -> new BidirectionalNode<>(this, 0, t, null))
                .sorted(fnNSort)
                .forEach(rootCollector::add);
        if (identityHashSet.size() != rootCollector.size()) {
            // 当该情况发生，只有可能是idHash中少于rootColl的情况
            String eqIdErrInfo = rootCollector.stream()
                    .map(UnsafeNode::data)
                    .map(t -> Stf.f("\n\t({}) -> IdentityHash: {}", t, identityHash(t)))
                    .collect(Collectors.joining());
            throw new IllegalArgumentException(
                    Stf.f("roots中包含相同的对象, roots:{}\n无法构建Tree",
                            eqIdErrInfo));
        }

        // 允许没有元素的树，若是，则无需下构建树子
        if (maxDepth == -1 || rootCollector.isEmpty()) {
            rootCollector.clear(); // 因为内部的List由ListFactory控制，遂该即使为空树，roots也不应引用Collections.emptyList
        } else {
            this.nodeCount = rootCollector.size();
            buildingTree(rootCollector, listFactory, fnCompGetChild, fnPreFilter,
                    listFactory.get(), // 复用临时存储直接child的，通常直接child不会太多
                    identityHashSet, // 直接地址，验证是否存在环或多个父
                    fnNSort, maxDepth - 1, 0);
        }
        this.root = rootCollector;
    }

    /**
     * 返回对象地址（无视hashcode重写）
     */
    private static int identityHash(Object o) {
        return System.identityHashCode(o);
    }

    /**
     * 构建树，若失败则抛出异常
     * <pre>
     *     {@code
     *     record Obj(int id, String name) {
     *         static Obj ofId(int id) {
     *             return new Obj(id, String.valueOf(id));
     *         }
     *     }
     *     List<Obj> objs = List.of(
     *             Obj.ofId(0), Obj.ofId(1), Obj.ofId(2),
     *             Obj.ofId(3), Obj.ofId(4), Obj.ofId(5)
     *     );
     *     Map<Obj, List<Obj>> getChild = Map.of(
     *             objs.get(0), List.of(objs.get(1)),
     *             objs.get(1), List.of(objs.get(2), objs.get(3)),
     *             objs.get(3), List.of(objs.get(4)),
     *             objs.get(4), List.of(objs.get(5))
     *     );
     *     Tree<Obj> treeObj = Tree.ofRoots(List.of(objs.get(0)),
     *             getChild::get,
     *             Comparator.comparingInt(Obj::id),
     *             ArrayList::new,
     *             getChild::containsKey,
     *             Predicate.isEqual(null).negate(),
     *             Integer.MAX_VALUE);
     *     String displayStr = treeObj.toDisplayStr();
     *     System.out.println(displayStr);
     *     // out:
     *     // │/
     *     // │└─ Obj[id=0, name=0]
     *     // │   └─ Obj[id=1, name=1]
     *     // │      ├─ Obj[id=2, name=2]
     *     // │      └─ Obj[id=3, name=3]
     *     // │         └─ Obj[id=4, name=4]
     *     // │            └─ Obj[id=5, name=5]
     *     }
     * </pre>
     *
     * @param roots              根节点，一个或多个
     * @param fnGetChild         函数，<b>要求给定一个实体，返回这个实体的直接子实体，返回的可迭代对象可以为null，也可以没有元素，
     *                           在使用默认{@code maxDepth}参数的情况下，
     *                           该方法一定要有穷，即一定能够找到逻辑上的叶子节点，否则可能会造成栈内存溢出</b>
     * @param fnSort             函数-实体排序的函数<b>(该函数仅在Tree构造时使用，Tree构造完成便被丢弃）</b>
     * @param fnPreNeedFindChild 函数(nullable)，要求给定一个实体，返回布尔值标识该实体是否需要寻找直接子实体，
     *                           该函数仅在对元素执行 {@code fnGetChild} 前执行，当该函数返回 {@code false}，
     *                           则 {@code fnGetChild} 函数一定不会对当前元素执行，否则，
     *                           函数 {@code fnGetChild} 一定会对当前元素执行，
     *                           这在 {@code fnGetChild} 过重时是一种优化<b>(该函数仅在Tree构造时使用）</b>
     * @param fnPreFilter        函数，预先处理树中实体，要求给定一个实体，当返回的布尔值为false时，
     *                           表示实体不加入树<b>(该函数仅在Tree构造时使用，Tree构造完成便被丢弃）</b>
     * @param maxDepth           <b>最大深度（包含），表示该树所承载的最大层级，
     *                           当已达到树所承载的最大深度时，即使树当前的叶子节点仍可通过 {@code fnGetChild} 获取子节点，
     *                           也不会将其添加到树中</b>
     * @throws NullPointerException     当不允许为空的参数给定空时
     * @throws IllegalArgumentException 当给定的最大停止深度小于 -1 时
     * @throws IllegalArgumentException 当构建树的过程发现循环边/循环引用/存在节点大于1个入度时
     */
    public static <E> Tree<E> ofRoots(Iterable<? extends E> roots,
                                      Function<? super E, ? extends Iterable<E>> fnGetChild,
                                      Comparator<? super E> fnSort,
                                      Predicate<? super E> fnPreNeedFindChild,
                                      Predicate<? super E> fnPreFilter,
                                      int maxDepth) {
        return new Tree<>(roots, fnGetChild, fnSort, ArrayList::new, fnPreNeedFindChild, fnPreFilter, maxDepth);
    }

    /**
     * 构建树，若失败则抛出异常<br>
     *
     * @param roots      根节点，一个或多个
     * @param fnGetChild 函数，<b>要求给定一个实体，返回这个实体的子实体，返回的可迭代对象可以为null，也可以没有元素，
     *                   该方法一定要有穷，即一定能够找到逻辑上的叶子节点，否则可能会造成栈内存溢出</b>
     * @throws NullPointerException     当不允许为空的参数给定空时
     * @throws IllegalArgumentException 当给定的最大停止深度小于 -1 时
     * @throws IllegalArgumentException 当构建树的过程发现循环边/循环引用/存在节点大于1个入度时
     */
    public static <E> Tree<E> ofRoots(Iterable<? extends E> roots,
                                      Function<? super E, ? extends Iterable<E>> fnGetChild) {
        final Predicate<? super E> defaultFnPre = (e) -> true;
        final Comparator<? super E> defaultFnSort = Comparator.nullsFirst(null);
        return Tree.ofRoots(roots, fnGetChild, defaultFnSort, defaultFnPre, defaultFnPre, Integer.MAX_VALUE);
    }

    /**
     * 给定一系列节点 {@code nodes}
     * 以给定的一系列节点做根节点，构建一颗树<br>
     * 给定的一系列树中的节点之间若存在间接或直接的父子关系，则构建树将失败 {@code R.Err(...)}<br>
     * 给定的一系列节点可以来自不同的树中
     * <pre>
     *     {@code
     *     Tree<T> tree1 = ...;
     *     Tree<T> tree2 = ...;
     *     Tree<T> sum12Tree = Tree.ofNodes(
     *          Stream.concat(tree1.root().stream(),
     *                        tree2.root().stream()).toList()).unwrap();
     *     int sum12NCount = sum12Tree.nodeCount()
     *     Assert.eq(tree1.nodeCount() + tree2.nodeCount(), sum12NCount);
     *     Iterator<T> it = tree1.iterator();
     *     while(it.hasNext()){
     *         it.next();
     *         it.remove();
     *     }
     *     Assert.eq(tree1.nodeCount(), 0);
     *     Assert.eq(sum12Tree.nodeCount(), sum12NCount);
     *     }
     * </pre>
     *
     * @param nodes 一系列树中的节点
     * @param <E>   type {@code node.data()}
     * @return {@code R.Ok(Tree)} | {@code R.Err(...)}
     * @apiNote 该方法不会影响提供 {@code nodes} 的树，返回的树仅持有相同的 {@link E} 的引用，
     * 不会持有给定的 {@code nodes} 中 节点的引用
     * @see #split()
     */
    public static <E> R<Tree<E>> ofNodes(Iterable<? extends Node<E>> nodes) {
        return R.ofFnCallable(() -> {
            // 使用IdentityHashMap，因为树中可能不同层级中有实体的equals值相同但其不为同一个节点node
            // 遂使用该map区分不同地址的对象
            Map<E, UnsafeNode<E>> selfRefNode = new IdentityHashMap<>();
            Iter.toStream(nodes) // 强转，不应有外界实现 Node，否则可能无法转为 NodeExt
                    .forEach(e -> selfRefNode.put(e.data(), (UnsafeNode<E>) e));
            return selfRefNode;
        }).map(selfRefNodeMap ->
                Tree.ofRoots(selfRefNodeMap.keySet(), (e) -> {
                    UnsafeNode<E> selfNode = selfRefNodeMap.get(e);
                    List<E> arrayList = null;
                    if (!selfNode.isLeaf()) {
                        // 非叶子节点，有子，子的子关系追加到map，并返回自己子
                        // 使用方法直接获取其子List引用，不使用接口暴露方法，即不创建中间容器
                        List<UnsafeNode<E>> childNode = selfNode.unsafeGetChildNode();
                        // 将 node解开，添加到滚动map selfRefNode 和 结果 arrayList中
                        arrayList = new ArrayList<>(childNode.size());
                        for (UnsafeNode<E> child : childNode) {
                            E data = child.data();
                            arrayList.add(data);
                            selfRefNodeMap.put(data, child);
                        }
                    }
                    // 将自身从selfRefNode map中移除，尽量防止map达到扩容大小而重新计算昂贵的SystemIdHash
                    selfRefNodeMap.remove(e);
                    return arrayList;
                }));
    }

    /**
     * 从线段关系构建树<br>
     * 若线段关系可以构建成树，返回 {@code R.Ok(Tree)}，
     * 否则返回构建树过程发生的异常 {@code R.Err(...)}<br>
     * 若给定的lines中至少有一个线段，且能够构建成树，则一定有:
     * <pre>
     *     {@code
     *     List<Line<E>> inLines = ...;
     *     Tree<E> tree = Tree.ofLines(inLines).unwrap();
     *     List<Line<E>> outLines = tree.toLines(LinkedList::new, Tree.Node::data).unwrap();
     *     Comparator<Line<E>> lineSort = ...;
     *     List<Line<E>> sortInLines = inLines.stream().sorted(lineSort).toList();
     *     List<Line<E>> sortOutLines = outLines.stream().sorted(lineSort).toList();
     *     Assert.eq(sortInLines, sortOutLines);
     *     }
     * </pre>
     *
     * @param lines n个线段
     * @param <E>   Line中元素类型
     * @return {@code R.Ok(Tree)} | {@code R.Err(...)}
     * @apiNote 若给定的lines为空、其中没有任何元素、线段构成的关系包含循环边、
     * 线段中有实体有一个以上父节点等，返回{@link R.Err}<br>
     * 该方法内会使用Line中元素做MapKey来计算父子关系，
     * 遂Line中元素类型是否实现equals和hashcode应在业务侧做考量，
     * 若错误使用该方法，可能导致需表达的拓扑结构与树表达的实际拓扑结构不一的情况
     * @see #toLines(Supplier, Function)
     */
    public static <E> R<Tree<E>> ofLines(Iterable<Line<E>> lines) {
        return R.ofFnCallable(() -> {
            Objects.requireNonNull(lines, "given lines is null");
            List<Line<E>> allLines = Iter.toStream(lines).toList();
            if (allLines.isEmpty()) {
                throw new IllegalArgumentException("given lines is empty");
            }
            boolean isTree = Line.isTree(allLines);
            if (!isTree) throw new IllegalArgumentException("Not a tree: " + allLines);
            Set<E> headerNodes = Line.findHeaderNodes(allLines);
            // MAYBE ？？？
            // 该map为一般map约束的使用equals做比较，而非直接地址值
            // 遂若Line中元素为实现eq&hash的类型，则可能破坏Lines构成的拓扑结构
            Map<E, List<E>> selfRefChildList = allLines.stream()
                    .collect(Collectors.groupingBy(
                            Line::begin,
                            Collectors.mapping(Line::end, Collectors.toList())
                    ));
            return Tree.ofRoots(headerNodes, selfRefChildList::get);
        });
    }

    /**
     * 将树中的节点关系用线段表达，返回 n 个线段 (n > 0)<br>
     * 若树中关系可以以 n 个 {@link Line} 形式表达，则返回 {@link R.Ok} 载荷 n 个 Line，
     * 否则返回 {@link R.Err} 载荷执行过程发生的异常 {@link RuntimeException}<br>
     * 若树为空树({@code tree.isEmpty()})，则返回 {@link R.Err} 载荷 {@link NoSuchElementException}<br>
     *
     * @param listFactory 函数-List构造方法引用，表示返回的承载n个Line的List实现类
     * @param fn          函数-能够接收一个{@link Node}，返回一个{@link V}，映射函数
     * @param <V>         通过函数转换的Line中承载的类型
     * @return Ok(Lines) | Err(...)
     * @apiNote 空树、仅包含根节点的树（因为没有子节点，无法构造{@link Line#end()})、
     * 给定的listFactory为空、给定的fn为空等，均会返回{@link R.Err}
     * @see #ofLines(Iterable)
     */
    public <V> R<List<Line<V>>> toLines(Supplier<? extends List<Line<V>>> listFactory,
                                        Function<? super Node<T>, ? extends V> fn) {
        return R.ofFnCallable(() -> {
            if (this.isEmpty()) {
                throw new NoSuchElementException("Tree is empty");
            }
            final List<Line<V>> r = listFactory.get();// get list type link? arr?
            final Queue<UnsafeNode<T>> queue = new LinkedList<>(); //BFS queue
            for (UnsafeNode<T> root : this.root) { // 先将root判定，添加到queue中
                if (root.isLeaf()) throw new IllegalStateException("tree just root node, can't build Line.end");
                queue.add(root);
            }
            while (!queue.isEmpty()) {
                UnsafeNode<T> n = queue.poll();
                // 因为queue中都是有叶子节点的，遂这里不用判定
                List<UnsafeNode<T>> childNodes = n.unsafeGetChildNode();
                for (UnsafeNode<T> child : childNodes) {
                    r.add(Line.of(n, child).map(fn));
                    if (!child.isLeaf()) { // 将有叶子节点的添加到队列中
                        queue.add(child);
                    }
                }
            }
            return r;
        });
    }


    /**
     * 返回一个 {@code Tree.Node} 迭代器<br>
     * 返回的迭代器是 <i>fail-fast</i> 的，
     * 可使用该迭代器以 BFS 方式从 {@link #root} 节点开始迭代该树中的所有节点<br>
     * 这个迭代器在调用 {@link Iterator#remove()} 方法时将会从树中删除节点及其自身的所有子节点，
     * 即一次 {@code remove()} 操作可能会删除 n 个元素（若当前节点为叶子节点，则仅会删除一个元素（自己），
     * 若非叶子节点，则会删除大于1个元素（自己及自己的子级））
     * <p>调用 {@link Iterator#remove()} 将会修改树的节点状态</p>
     *
     * @return 树的迭代器
     * @apiNote 该迭代器并非线程安全，当树的节点状态发生改变后，
     * 非引发变动的实例 ({@link TreeIter} 便会失效，
     * 调用这些失效实例的任何方法都会抛出 {@link ConcurrentModificationException}
     * @see ConcurrentModificationException
     * @see Tree#nodeDataIterator()
     */
    @SuppressWarnings("NullableProblems")
    @Override
    public Iterator<Node<T>> iterator() {
        return new TreeIter<>(this);
    }

    /**
     * 返回一个 {@code Tree.Node.data} 迭代器<br>
     * 返回的迭代器是 <i>fail-fast</i> 的，
     * 可使用该迭代器以 BFS 方式从 {@link #root} 节点开始迭代该树中的所有节点中的 {@code data}<br>
     * 与 {@link Tree#iterator()} 返回的迭代器不同，该迭代器不允许调用 {@code remove} 方法，
     * 因为该调用语义不明确（被该迭代器迭代的元素仅代表Tree.Node.data，不代表Tree.Node，遂没有从Tree删除其Node的语义）
     *
     * @return 树的迭代器
     * @throws UnsupportedOperationException 当调用该迭代器的 {@code remove()} 方法
     * @apiNote 该迭代器并非线程安全，当树的节点状态发生改变后，
     * 非引发变动的实例 ({@link TreeIter} 便会失效，
     * 调用这些失效实例的任何方法都会抛出 {@link ConcurrentModificationException}
     * @see Tree#iterator()
     */
    public Iterator<T> nodeDataIterator() {
        return MappedIter.of(new TreeIter<>(this) {
            @Override
            public void remove() {
                throw new UnsupportedOperationException(
                        "在Tree的nodeDataIterator迭代器上调用remove方法的语义不明确，不允许该操作," +
                        "若需在迭代时删除树中节点，使用Tree.iterator()");
            }
        }, Node::data);
    }


    /**
     * 树的迭代器 （<i>fail-fast</i>)<br>
     * BFS 方式，内有 Queue 作为对当前层节点的子节点的缓存
     *
     * @apiNote 该迭代器并非线程安全，当树的节点状态发生改变后，
     * 非引发变动的实例 ({@link TreeIter} 便会失效，
     * 调用这些实例的任何方法都会抛出 {@link ConcurrentModificationException}
     * @implNote 该迭代器的 Queue 中还有作为 fence（栅栏）的标记节点，该节点外界不可见，
     * fence 仅是为了隔离不同父节点的一批节点，遂对外界安全的不可见
     */
    private static class TreeIter<E> implements Iterator<Node<E>> {
        // 有自己所在父级的childNodes这个List本身的引用，用以在删除自身的时候使用
        // 有自己的上一个被next的Node的ChildNodes本身的List的引用，当自身被next到，则将上一个next的child都添加到队列中
        // 队列内有存有非使用方的Node, 而是当前迭代器存的栅栏对象，
        // 当前迭代器 存的栅栏的性质：depth为 -114514 ，data为null，childNodes引用的是
        // 从当前栅栏之后到下一个栅栏之前的所有node的父的CN（ChildNodes）List的引用
        // fenceNode的parentNode有值（不为root节点时）
        // fence隔开不同父的队列中的node，每当拿到栅栏，切换当前父 currentParentCNRef
        private static final int FENCE_DEPTH = -114514;

        /**
         * 新建一个Node作为隔离栅栏，
         * 该node内存有队列中该fenceNode之后到下一个fenceNode之前的node的父node引用以及父node的子集合的引用
         */
        UnsafeNode<E> newFence(List<UnsafeNode<E>> parentNodeChildNodesListRef, UnsafeNode<E> parentNodeRef) {
            // 能调用到该方法，则该参数的List一定不为null并且不为empty
            BidirectionalNode<E> fenceNode = new BidirectionalNode<>(null, FENCE_DEPTH, null, parentNodeRef);
            fenceNode.childNodes = parentNodeChildNodesListRef;
            return fenceNode;
        }

        /**
         * 测试 node 是否为 栅栏
         */
        boolean isFence(UnsafeNode<E> node) {
            return node.depth() == FENCE_DEPTH;
        }

        final Tree<E> treeRef;
        final Queue<UnsafeNode<E>> queue = new LinkedList<>(); // 滚动

        // modifyFlag 用以验证是否有同时使用多个迭代器对一个树做修改
        int modifyCount;

        UnsafeNode<E> currentParentNodeRef; // fence后节点的父节点引用
        UnsafeNode<E> beforeNodeRef; // 前一个node引用
        List<UnsafeNode<E>> currentParentChildNodesRef; // 当前的node的父级的childNodesList的引用
        List<UnsafeNode<E>> beforeNodeChildNodesRef; // 上一个node可能为叶子节点 , 所以该，可能为null

        UnsafeNode<E> current; // 当前被next的node的引用，用以从currentParentChildNodesRef中remove时使用

        TreeIter(Tree<E> tree) {
            this(tree, tree.root);
        }

        /*
        20250529 增加该方法，将树的迭代器的开始迭代的Node与Tree.root分离，
        从给定的 iterStartNodes 开始迭代，这些Node应当是同一层（depth eq）（可以不是共同父），
        否则可能造成Node被反复迭代到
         */
        TreeIter(Tree<E> tree, List<UnsafeNode<E>> iterStartNodes) {
            this.treeRef = tree;
            this.queue.addAll(iterStartNodes);
            this.currentParentNodeRef = null; //root no parent
            this.currentParentChildNodesRef = iterStartNodes;
            this.beforeNodeRef = null;
            this.beforeNodeChildNodesRef = null;
            this.modifyCount = tree.modifyCount;
            // root 添加后不必添加Fence，因为root的currentParentChildNodesRef已经有引用了
        }

        /**
         * 若修改计数与树的对不上，抛异常
         */
        final void assertJustMeModifyTree() {
            if (treeRef.modifyCount != this.modifyCount) {
                throw new ConcurrentModificationException("Tree 已被其他实体修改，当前迭代器已失效");
            }
        }

        @Override
        public boolean hasNext() {
            assertJustMeModifyTree();
            // hasNext仅查询，不会改变任何标记
            // 无需查看  isFence（queue.peek()）？，因为有fence，证明fence后面一定有元素
            // 即有next的条件是：队列不为empty，或者，beforeCNRef有引用
            // 无需查看beforeCNRef是否为empty，因为没有子的beforeNode不会将自身的CNRef引用赋值给该引用
            return (!queue.isEmpty()) || (beforeNodeChildNodesRef != null);
        }

        @Override
        public Node<E> next() {
            assertJustMeModifyTree();
            // 因为 hasNext判定逻辑里有 beforeNodeCNRef 所以，当当前queue为空时，上一个node的ChildNodes便可以添加到
            // queue了，因为上一个node已经走过了
            // 无需判断queue，before的添加到queue里， 并将before的置位null
            if (beforeNodeChildNodesRef != null) {
                UnsafeNode<E> fenceNode = newFence(beforeNodeChildNodesRef, beforeNodeRef);
                queue.add(fenceNode);
                queue.addAll(beforeNodeChildNodesRef);
                beforeNodeChildNodesRef = null;
                beforeNodeRef = null;
            }


            // 可能取出来的是标记对象，这里应当用remove？ no！因为还有current引用，若这里抛出异常
            // 则调用remove时会将未被重置的current（也就是上一个）执行删除，这会破坏迭代器的remove语义
            // ---- 这里也可用remove，只需再iter的该next方法头将current置位null即可
            UnsafeNode<E> nodeOrFence = queue.poll();
            if (nodeOrFence == null) {
                current = null;
                // 将所有引用丢弃
                currentParentChildNodesRef = null;
                beforeNodeChildNodesRef = null;
                currentParentNodeRef = null;
                beforeNodeRef = null;
                throw new NoSuchElementException("Iterator is empty");
            }

            // 实际的 current
            UnsafeNode<E> n;

            // 因为该方法之前会将beforeNodeChildNodesRef引用中的node都添加到队列，若队列为空，则证明
            // 当前node的任何一个上一个node都没有子，也就是树已经走完了
            if (isFence(nodeOrFence)) {  // 若是栅栏，则切换自己父的引用
                // 因为已经判定了是fenceNode，所以一定为UnidirectionalNode，并且其持有的
                // childNodes 就是队列中当前fenceNode之后，下一个fenceNode之前的所有node的父引用的CNRef
                // UnidirectionalNode已修改为BidirectionalNode,因为要持有父的引用
                currentParentChildNodesRef = nodeOrFence.unsafeGetChildNode();
                currentParentNodeRef = nodeOrFence.unsafeGetParentNode();

                // 20250527 因为 node 拥有了 destroy 情况，
                // 遂可能其他迭代器已经删除其或其父，导致其自身也被标记删除，
                // 但仍在当前迭代器缓存队列中，遂若为 destroy 的，则丢弃引用并重新获取
                // 若直到最后都没找到一个可用的，说明已经遍历完成了？
                // 不可，因为hasNext为true，就应当返回一个元素，而不是抱有侥幸心理行事
                // 当 发现destroy，应当立即抛出异常

                n = queue.poll(); // 若是标记对象，再取一个 , 根据逻辑，一定不会有两个fence连着
                if (n == null || isFence(n)) throw new ConcurrentModificationException();
                if (n.isPruned()) {
                    // 将所有引用丢弃
                    current = null;
                    currentParentChildNodesRef = null;
                    beforeNodeChildNodesRef = null;
                    currentParentNodeRef = null;
                    beforeNodeRef = null;
                    throw new ConcurrentModificationException("Tree 已被其他实体修改, 当前迭代器已迭代到被删除的节点");
                }

                // 因为当前为fence，而fence后一定有实际node, 遂下述n.isLeaf无需判n是否为null
            } else {
                n = nodeOrFence; //若不是，则赋值给n
            }
            // 这里获取到实际的node，并且currentParentChildNodesRef已经切换为该node所在的父的CNRef
            // 判定当前n是否为叶子节点，若不是，则将自身的CNRef给before引用
            if (!n.isLeaf()) {
                beforeNodeChildNodesRef = n.unsafeGetChildNode(); // 获取childNodes的直接引用
                beforeNodeRef = n;
            }
            current = n; // iter内暂存被next的node的引用，用以删除时使用
            return n;
        }

        /**
         * 该方法会修改树的节点状态
         */
        @Override
        public void remove() {
            assertJustMeModifyTree();
            // remove当前被next迭代到的node的意义就是：
            // 从当前node的父节点的CNRef中删除自己
            // 无需判定是否为双引用节点并将自身的parentNode引用置位null，
            // 因为自己持有自己父的引用不会阻止垃圾回收的GCRoot可达性判定
            // 为help GC，后续或可优化，Iter拿得到当前的nodeType，当为BINode双向引用时
            // 且不为root时，可选择清除对父的引用
            if (current != null) {
                // fix me 自己被删除，但在next方法内自己将beforeNodeChildNodesRef设定为自己的
                //  childNodes，在下次next时会被触发造成自己没了，但迭代器还在迭代自己的子
                beforeNodeChildNodesRef = null;
                beforeNodeRef = null;
                // fix me modify depth and nodeCount
                treeRef.pruneNodeAndUpdateTreeState(
                        this.current,
                        this.currentParentNodeRef,
                        this.currentParentChildNodesRef, true);
                current = null; //将current引用置位null
                this.modifyCount = this.treeRef.modifyCount; // 成功修改 将自身的修改计数 与树相同
            } else {
                throw new IllegalStateException("not found node");
            }
        }
    }

    /**
     * 剪枝
     * <p>该方法会修改树的节点状态</p>
     *
     * @param n 需被剪枝的节点
     * @see #pruneNodeAndUpdateTreeState(UnsafeNode, UnsafeNode, List, boolean)
     * @see #pruneNodeAndUpdateTreeState(UnsafeNode, UnsafeNode)
     */
    private void pruneNodeAndUpdateTreeState(Node<T> n) {
        UnsafeNode<T> un = (UnsafeNode<T>) n;
        if (n.isRoot()) {
            pruneNodeAndUpdateTreeState(un, null, this.root, true);
        } else {
            pruneNodeAndUpdateTreeState(un, un.unsafeGetParentNode());
        }
    }


    /**
     * 剪枝，从父的子中移除，并且更新树的节点状态值<br>
     * 该方法不会有{@link #pruneNodeAndUpdateTreeState(UnsafeNode, UnsafeNode, List, boolean)} 方法API描述的问题
     * <p>该方法会修改树的节点状态</p>
     *
     * @param pruneNode  要剪掉的node
     * @param parentNode 要dump的node的父节点
     * @throws NullPointerException  parentNode is null
     * @throws IllegalStateException parentNode is leaf
     */
    private void pruneNodeAndUpdateTreeState(UnsafeNode<T> pruneNode, UnsafeNode<T> parentNode) {
        Objects.requireNonNull(parentNode, "given parentNode is null");
        if (parentNode.isLeaf()) {
            throw new IllegalStateException("given parentNode is leaf");
        } else {
            pruneNodeAndUpdateTreeState(pruneNode, parentNode,
                    parentNode.unsafeGetChildNode(), true);
        }
    }


    /**
     * 剪枝，从父的子中移除，并且更新树的节点状态值
     * <p>该方法会修改树的节点状态</p>
     *
     * @param pruneNode                  要剪掉的node
     * @param nullableParentNodeRef      要dump的node的父节点（nullable（当为root节点时））
     * @param parentChildNodesListRef    父的子集合
     * @param ifPossibleDumpEmptyListRef 选项，若为true，则如果可以，当 pruneNode 被丢弃后，
     *                                   被丢弃的node的父的子List为empty时，将该List引用丢弃，
     *                                   （无论如何，该不会丢弃roots所在的List的引用）
     * @throws IllegalStateException    已被剪枝的节点再次被剪枝
     * @throws NullPointerException     parentChildNodesListRef is null
     * @throws IllegalStateException    parentChildNodesListRef not found pruneNode ref
     * @throws IllegalArgumentException given parentChildNodesRef is not from parentNodeRef
     * @apiNote 该方法参数的 nullableParentNodeRef 和 parentChildNodesListRef 是分别给方法的，
     * 遂当该方法的外界持有 parentChildNodesListRef 的引用时，当 parentChildNodesListRef 为empty，
     * 且 ifPossibleDumpEmptyListRef 为 true，且 nullableParentNodeRef 不为null，
     * 则其在第一次调用该方法后，其 ParentNode 的对子List的引用就已经为null了，
     * 外界持有的 parentChildNodesListRef 就已不是 Tree 中 ParentNode 逻辑意义上的子List了，
     * 遂外界不能持有 parentChildNodesListRef 的引用，
     * 也不应当在一个循环代码块内持有 parentChildNodesListRef 并反复调用该方法
     */
    @SuppressWarnings("SameParameterValue")
    private void pruneNodeAndUpdateTreeState(
            UnsafeNode<T> pruneNode,
            UnsafeNode<T> nullableParentNodeRef,
            List<UnsafeNode<T>> parentChildNodesListRef,
            boolean ifPossibleDumpEmptyListRef
    ) {
        if (pruneNode.isPruned()) {
            throw new IllegalStateException("node is pruned");
        }
        Objects.requireNonNull(parentChildNodesListRef);
        if (!parentChildNodesListRef.contains(pruneNode)) {
            // 若父的子集合中无当前，则直接用异常中断后续逻辑，防止更新树全局变量
            // fix：还有一点，若同一时间持有树的两个迭代器，则第二个迭代器在进行删除操作时
            //   可能 需被删除的节点 已经被删除（因为被第一个迭代器删除的节点可能已存在于
            //   第二个迭代器的 BFS 缓存队列中，这时，被删除的节点将在第二个迭代器上可见），
            //   若未有该层校验，则可能造成树的状态值异常刷新 eg treeDepth and nodeCount...
            throw new IllegalStateException(
                    "Node not found in parent's children list");
        }
        // 使用两个变量记录被剪枝的node上的count和depth，用以更新Tree的count和depth
        final int[] countAndDeepLeafDepth = {0, 0};
        // 从要删除的node bfs 遍历 局部bfs
        bfs(() -> List.of(pruneNode),
                n -> {
                    countAndDeepLeafDepth[0] = countAndDeepLeafDepth[0] + 1; //count ++
                    // 因为为bfs，所以最后的点一定深度最深，所以无需Math.max比较
                    countAndDeepLeafDepth[1] = n.depth(); // until leaf depth update
                },
                FN_ACC_DO_NOTHING,
                FN_PRUNE_NODE);
        // 完成后 里面就有 count 和 最深叶子节点的depth了
        this.nodeCount -= countAndDeepLeafDepth[0]; //tree nodeCount 更新
        int deepLeafDepth = countAndDeepLeafDepth[1];
        // 删除自身引用
        parentChildNodesListRef.remove(pruneNode);
        // 将树的变更记录 + 1
        incrementModifyCount();

        // 优化：若parentChildNodes已经为empty（即被丢弃的node是其唯一的子），则丢弃空集合引用
        // 这里拿不到parentNode ... 做不到，下面有bfs，可以下面拿...

        boolean emptyListRefWasDump = false;
        if (nullableParentNodeRef != null // 这里无需判定是否为root的List，因为root没有parentNode
            && ifPossibleDumpEmptyListRef
            && parentChildNodesListRef.isEmpty()) {
            if (nullableParentNodeRef.unsafeGetChildNode() == parentChildNodesListRef) {
                // 若不是父的子List，抛出异常，否将引用置位null
                nullableParentNodeRef.unsafeSetChildNode(null);
                emptyListRefWasDump = true;
            } else {
                throw new IllegalArgumentException("given parentChildNodesRef is not from parentNodeRef");
            }
        }


        // tree.depth 更新，分情况优化:
        // 首先任何node的depth不可能大于tree.depth 所以分小于和等于情况(多线程写状态可能大于，该情况下已做校验):
        // 若 deepLeafDepth 小于 tree.depth 证明该dropNode到最后都没有达到树的深度，则无需更新tree的depth
        // 若 deepLeafDepth 等于 tree.depth 则证明该枝叶可能是树的最深了，需更新树的depth
        final int treeDepth = this.depth; // 树深度
        // 多线程修改树的情况，可能造成最深的叶子节点大于树深度
        Err.realIf(deepLeafDepth > treeDepth, IllegalStateException::new,
                "非法的树状态：当前树最大深度的叶子节点深度值大于树深度：deepLeafDepth: {}, tree.depth: {}，该树非线程安全，不能在多个线程中同时修改树",
                deepLeafDepth, treeDepth);
        if (deepLeafDepth == treeDepth) {

            // 该为最坏情况，树的最大深度在被剪枝的node上，则需要dfs更新树深度
            // 先剪掉
            // 复用之前创建的 int, 这次就不用更新count了，因为count已经被计算了，
            // 遂这里只更新其 depth，但我选择将 depth 存储在 [0] 位置上
            countAndDeepLeafDepth[0] = -1; // reset
            // fix me 重置为0时，若下bfs不进行至少一次，且树只有一个root时，空树的depth会被置为0，已修复，现reset为-1
            // reused array0 update tree.deepLeafDepth ，全局tree bfs 重新计算树depth

            // 若父级CNList已经空了，且选项开启，则尝试丢弃父级的CNList
            if ((!emptyListRefWasDump)
                && ifPossibleDumpEmptyListRef
                && parentChildNodesListRef.isEmpty() // 仅当不为roots的List时丢弃
                && parentChildNodesListRef != this.root) {
                // 要获取直接的ChildNodes引用不能用isLeaf判定，因为其会把emptyList忽略
                // 因为下直接获取ChildNodes引用，遂可能不为null而为empty，这时就不能在nullableChild == null块内更新depth了
                this.forEachBfs(n -> {
                    UnsafeNode<T> unsafeN = (UnsafeNode<T>) n;
                    countAndDeepLeafDepth[0] = n.depth();
                    // to do unIsLeaf，因为空集合也会被判定为Leaf，所以应当直接获取其引用
                    // 因为parentChildNodes这时候不可能等于null，遂可以直接判断是否相等 ==
                    if (parentChildNodesListRef == unsafeN.unsafeGetChildNode()) {
                        // 若找到了自己的父级 且为 empty
                        unsafeN.unsafeSetChildNode(null); // 丢弃引用
                    }
                });
                // 否则，仅计算新的depth
            } else {
                // 因为为bfs，所以最后的点一定深度最深，所以无需Math.max比较
                this.forEachBfs(n -> countAndDeepLeafDepth[0] = n.depth());
            }
            // 更新树深
            this.depth = countAndDeepLeafDepth[0];
        }
    }

    /**
     * 使用当前树的根节点，以每个根节点做为单独的根，构建多个树<br>
     * 情况细分:
     * <ul>
     *     <li>若当前树为空树 {@code tree.rootCount() == 0}，则返回空List {@code Collections.emptyList()}</li>
     *     <li>若当前树有1个根节点，则返回只有一个元素的List {@code Collections.singletonList()}</li>
     *     <li>若当前树有n个根节点，则返回含有n个元素的List {@code unmodifiableList},
     *     其中{@code unmodifiableList.size() == tree.rootCount()}</li>
     * </ul>
     * 一定有:
     * <pre>
     *     {@code
     *     Tree<T> tree = ...;
     *     List<Tree<T>> spTree = tree.split();
     *     Tree<T> newTree = Tree.ofNodes(spTree.stream()
     *          .map(Tree::root)
     *          .flatMap(List::stream)
     *          .toList()).unwrap();
     *     Assert.eq(tree.nodeCount(), newTree.nodeCount());
     *     Assert.eq(tree.depth(), newTree.depth());
     *     }
     * </pre>
     *
     * @return emptyList | n Tree
     * @apiNote 该方法不会影响当前树状态，对返回的新树进行修改也不会影响当前树
     * @see #ofNodes(Iterable)
     */
    public List<Tree<T>> split() {

        List<UnsafeNode<T>> rts = this.root;
        if (rts.isEmpty()) {
            return Collections.emptyList();
        } else if (rts.size() == 1) {
            // just one root，return copy One
            return Collections.singletonList(Tree.ofNodes(rts).unwrap());
        } else {
            // 多个 因为互不影响，且不影响原树，多线程走
            List<CompletableFuture<Tree<T>>> fl = rts.stream()
                    .map(List::of)
                    .map(sl ->
                            CompletableFuture.supplyAsync(() ->
                                    Tree.ofNodes(sl).unwrap()))
                    .toList();
            return fl.stream()
                    .map(R::ofFuture)
                    .map(R::unwrap)
                    .toList();
        }
    }


    /**
     * 递归-DFS<br>
     * 根据给定的函数操作，可表示先序或后序遍历
     *
     * @param node                 树节点
     * @param fnPreAcc             函数-先序遍历访问
     * @param fnPostAccIfNotPruned 函数-后序遍历访问
     * @throws NullPointerException 给定的节点为空或函数为空时
     */
    private void dfs(UnsafeNode<T> node,
                     Consumer<? super UnsafeNode<T>> fnPreAcc,
                     Consumer<? super UnsafeNode<T>> fnPostAccIfNotPruned) {
        fnPreAcc.accept(node); // 先访问
        if (node.isPruned()) return; // 若该访问修改了树为删除状态，则后续方法无需再调用
        if (!node.isLeaf()) {
            List<UnsafeNode<T>> cList = node.unsafeGetChildNode();
            for (UnsafeNode<T> child : cList) {
                dfs(child, fnPreAcc, fnPostAccIfNotPruned);// 递归子
            }
        }
        fnPostAccIfNotPruned.accept(node); // 后访问
    }

    /**
     * 非递归-BFS遍历
     *
     * @param fnGetNodes 树节点提供函数，提供n个树节点，这些节点应是同一深度的，
     *                   否则可能会导致一个实体被遍历不止一次
     * @param fn         函数
     * @throws NullPointerException 给定的节点为空或函数为空时
     */
    private void bfs(Supplier<? extends Iterable<UnsafeNode<T>>> fnGetNodes,
                     Consumer<? super UnsafeNode<T>> fn) {
        this.bfs(fnGetNodes, fn, FN_ACC_DO_NOTHING, FN_ACC_DO_NOTHING);
    }

    /**
     * 非递归-BFS遍历
     *
     * @param fnGetNodes             树节点提供函数，提供n个树节点，这些节点应是同一深度的，
     *                               否则可能会导致一个实体被遍历不止一次
     * @param fnPreAcc               函数，一定对node执行
     * @param fnPostChildAddQueueAcc 函数，仅对有子节点的node执行，在被遍历的node的子添加到bfs队列后执行
     * @param fnPostAccIfNotPruned   函数，一定对node执行
     * @throws NullPointerException 给定的节点为空或函数为空时
     */
    private void bfs(Supplier<? extends Iterable<UnsafeNode<T>>> fnGetNodes,
                     Consumer<? super UnsafeNode<T>> fnPreAcc,
                     Consumer<? super UnsafeNode<T>> fnPostChildAddQueueAcc,
                     Consumer<? super UnsafeNode<T>> fnPostAccIfNotPruned) {
        Objects.requireNonNull(fnGetNodes, "fnGetNodes is null");
        Objects.requireNonNull(fnPreAcc, "fnPreAcc is null");
        Objects.requireNonNull(fnPostAccIfNotPruned, "fnPostAccIfNotPruned is null");
        Objects.requireNonNull(fnPostChildAddQueueAcc, "fnPostChildAddQueueAcc is null");
        Queue<UnsafeNode<T>> queue = new LinkedList<>();
        Iterable<UnsafeNode<T>> iter = fnGetNodes.get();
        for (UnsafeNode<T> node : iter) {
            queue.add(node);
        }
        while (!queue.isEmpty()) {
            UnsafeNode<T> current = queue.poll();
            fnPreAcc.accept(current); // pre 处理当前节点
            if (current.isPruned()) continue; // 若使用完fnPreAcc后为 isPruned 则不将自身子添加到bfs队列
            // 将当前节点的所有子节点加入队列
            if (!current.isLeaf()) {
                queue.addAll(current.unsafeGetChildNode());
                fnPostChildAddQueueAcc.accept(current);
            }
            fnPostAccIfNotPruned.accept(current); // 最后对current执行
        }
    }

    /**
     * 使用给定函数排序当前树中同属一个父的所有子<br>
     * 这会改变树中同属一个父的存储子的List中元素顺序
     * <p>该方法会修改树的节点状态</p>
     *
     * @param fnSort 排序函数
     * @return this
     */
    public Tree<T> sort(Comparator<? super T> fnSort) {
        Objects.requireNonNull(fnSort, "fnSort is null");
        Comparator<Node<T>> comparator = compNSortFn(fnSort);
        this.root.sort(comparator);
        this.bfs(() -> root, FN_ACC_DO_NOTHING,
                // 调用该方法时子已经被添加到 队列且一定有子，
                // 但不会影响sort逻辑，因为仅是同层排序
                (n) -> n.unsafeGetChildNode().sort(comparator),
                FN_ACC_DO_NOTHING);
        // 因为仅排序不会增删树中节点，但确确实实修改了树，遂需要增加修改计数
        // 需注意，可能排序完后顺序与原本顺序相同，这种情况虽然可以追溯判定
        // 但这里BFS就不对每次排序判定了，直接认为树被修改，更新incrementModifyCount值
        incrementModifyCount();
        return this;
    }

    /**
     * 使用给定函数剪掉树中 {@code fnTest.test(n) == false} 的节点
     * <p>若节点被剪掉，则其所有子节点也会从树中被剪掉
     * <p>该方法会修改树的节点状态</p>
     *
     * @param fnTest 函数
     * @return this
     */
    public Tree<T> filter(Predicate<? super Node<T>> fnTest) {
        // 该方法内无需调用 incrementModifyCount，因为 prune 内会调用之
        Objects.requireNonNull(fnTest, "fnTest is null");
        this.bfs(() -> root,
                // 仅将不符合的删除
                (un) -> {
                    boolean test = fnTest.test(un);
                    if (!test) un.prune();
                },
                FN_ACC_DO_NOTHING, FN_ACC_DO_NOTHING);
        return this;
    }

    private void incrementModifyCount() {
        this.modifyCount += 1;
    }

    /**
     * 非递归-BFS遍历
     *
     * @param fn 函数-访问每个被遍历到的{@link MutNode}
     * @throws NullPointerException 给定的节点为空或函数为空时
     * @apiNote 函数会访问每个节点，除非清楚在做什么，否则不应该在函数中通过node递归的访问其父或子
     */
    public void forEachBfs(Consumer<? super Node<T>> fn) {
        bfs(() -> root, fn);
    }

    /**
     * 递归-DFS先序遍历
     *
     * @param fn 函数-访问每个被遍历到的{@link MutNode}
     * @throws NullPointerException 给定的节点为空或函数为空时
     * @apiNote 函数会访问每个节点，除非清楚在做什么，否则不应该在函数中通过node递归的访问其父或子
     */
    public void forEachDfsPreOrder(Consumer<? super Node<T>> fn) {
        for (UnsafeNode<T> root : root) {
            dfs(root, fn, FN_ACC_DO_NOTHING);
        }
    }

    /**
     * 递归-DFS后序遍历
     *
     * @param fn 函数-访问每个被遍历到的{@link MutNode}
     * @throws NullPointerException 给定的节点为空或函数为空时
     * @apiNote 函数会访问每个节点，除非清楚在做什么，否则不应该在函数中通过node递归的访问其父或子
     */
    public void forEachDfsPostOrder(Consumer<? super Node<T>> fn) {
        for (UnsafeNode<T> root : root) {
            dfs(root, FN_ACC_DO_NOTHING, fn);
        }
    }

    // DEF FUNCTION ===========================

    /**
     * compose nullable hasChildFn and getChildFn
     */
    private Function<? super T, ? extends Iterable<T>>
    compGetChildFn(Predicate<? super T> nullableFnPreNeedFindChild,
                   Function<? super T, ? extends Iterable<T>> fnGetChild) {
        // 为 null 的去除掉，不允许Tree中Node的data为null
        return nullableFnPreNeedFindChild == null ?
                (e) -> e != null ? fnGetChild.apply(e) : null :
                (e) -> (e != null && nullableFnPreNeedFindChild.test(e)) ? fnGetChild.apply(e) : null;
    }

    /**
     * 将 比较T类型的 函数 转为 比较Node[T]类型的 函数
     */
    private Comparator<Node<T>>
    compNSortFn(Comparator<? super T> sort) {
        return (n1, n2) -> sort.compare(n1.data(), n2.data());
    }
    // DEF FUNCTION ===========================

    /**
     * 递归构造树
     *
     * @param nodes                   节点
     * @param listFactory             函数-List构造方法引用
     * @param fnGetChild              函数-从实例获取其子
     * @param fnFilterTest            函数-跳过不符合条件的实例
     * @param tempNodeRefReusableList 复用的临时List，存储Node引用
     * @param fnNSort                 函数-排序Node
     * @param maxDepth                最大停止深度（包含）
     * @param currentDepth            当前深度
     */
    private void buildingTree(List<UnsafeNode<T>> nodes,
                              Supplier<? extends List<UnsafeNode<T>>> listFactory,
                              Function<? super T, ? extends Iterable<? extends T>> fnGetChild,
                              Predicate<? super T> fnFilterTest,
                              List<UnsafeNode<T>> tempNodeRefReusableList,
                              Set<T> tempIdentityHashSet,
                              Comparator<Node<T>> fnNSort,
                              int maxDepth,
                              int currentDepth) {
        // 到达最大层 直接返回
        this.depth = Math.max(this.depth, currentDepth);
        if (maxDepth < 0) return;
        // 已被fnFilterTest走过的node
        for (UnsafeNode<T> node : nodes) {
            T ref = node.data();
            Iterable<? extends T> it = fnGetChild.apply(ref);
            if (it != null) {
                // 清理临时持有Node引用的集合
                tempNodeRefReusableList.clear();
                for (T child : it) {

                    // fix 使该校验移至tempIdentityHashSet判定前，
                    // 否则可能造成没有在Tree中，但却在tempNodeRefReusableList中的情况
                    // 若元素未能通过test，则不将其纳入
                    if (!fnFilterTest.test(child)) {
                        continue;
                    }

                    // CHECK 还未添加到 tempIdentityHashSet 便发现自身的引用
                    // 即证明：1. 有循环边 or 2. 当前实体有两个父节点
                    if (tempIdentityHashSet.contains(child)) {
                        int refIdHash = identityHash(ref);
                        int childIdHash = identityHash(child);
                        throw new IllegalArgumentException(Stf
                                .f("""
                                        获取对象 '{}'({}) 的子对象的过程中，
                                        发现其子对象 '{}'({}) 已经存在于树中，可能的情况:
                                        \t1.存在循环边/循环引用(环)
                                        \t2.'{}'({}) 有多个父对象
                                        无法构建Tree""", ref, refIdHash, child, childIdHash, child, childIdHash)
                        );
                    } else {
                        // 没有，则将自身加入 identityHashSet中，表示自身已被纳入tree
                        tempIdentityHashSet.add(child);
                    }
                    // 走到这里，即迭代器不为null，且内有通过test的元素
                    UnsafeNode<T> n = new BidirectionalNode<>(this, currentDepth + 1, child, node);
                    tempNodeRefReusableList.add(n);
                    this.nodeCount += 1;
                }
                if (!tempNodeRefReusableList.isEmpty()) {
                    List<UnsafeNode<T>> childCollector = listFactory.get();
                    tempNodeRefReusableList.stream().sorted(fnNSort).forEach(childCollector::add);
                    // 向下递归
                    buildingTree(childCollector, listFactory, fnGetChild, fnFilterTest,
                            tempNodeRefReusableList,
                            tempIdentityHashSet,
                            fnNSort,
                            maxDepth - 1,
                            currentDepth + 1);
                    node.unsafeSetChildNode(childCollector);
                }
            }

        }
    }

    /**
     * 递归构造显示树的字符串（从根节点直到叶子节点）<br>
     * 若树为空树，则返回的字符串仅包含 {@code /}
     * <pre>
     *     {@code
     *     List<Line<Integer>> lines = List.of(
     *             Line.of(1, 2),
     *             Line.of(2, 3),
     *             Line.of(3, 4),
     *             Line.of(1, 5),
     *             Line.of(5, 6)
     *     );
     *     Tree<Integer> tree = Tree.ofLines(lines).unwrap();
     *     System.out.println(tree.toDisplayStr());
     *     // out:
     *     // │/
     *     // │└─ 1
     *     // │   ├─ 2
     *     // │   │  └─ 3
     *     // │   │     └─ 4
     *     // │   └─ 5
     *     // │      └─ 6
     *     }
     * </pre>
     *
     * @return 显示树的字符串
     * @see #toDisplayStr(int, Function)
     */
    public String toDisplayStr() {
        return toDisplayStr(depth(), n -> Objects.toString(n.data()));
    }

    /**
     * 递归构造显示树的字符串<br>
     * <pre>
     *     {@code
     *     Tree<T> tree = ...;
     *     String defaultDisplay = tree.toDisplayStr();
     *     String customDisplay = tree.toDisplayStr(tree.depth(), n -> n.data().toString());
     *     Assert.eq(defaultDisplay, customDisplay);
     *     }
     * </pre>
     *
     * @param displayDepth     要显示的截止深度（包含）
     * @param fnNodeDisplayFmt 函数-会访问每个节点，描述node显示为字符串的形式（通常只需要 {@code node.data()})
     * @return 显示树的字符串
     * @throws IllegalArgumentException 当给定的截止深度小于-1时
     * @throws NullPointerException     给定的函数为空时
     * @apiNote 函数会访问每个节点，除非清楚在做什么，否则不应该在函数中通过node递归的访问其父或子
     * @see #toDisplayStr()
     */
    public String toDisplayStr(int displayDepth,
                               Function<? super Node<? super T>, ? extends CharSequence> fnNodeDisplayFmt) {
        Objects.requireNonNull(fnNodeDisplayFmt, "fnNodeDisplayFmt is null");
        Err.realIf(displayDepth < -1, IllegalArgumentException::new, "displayDepth < -1");
        if (displayDepth == -1 || isEmpty()) return Const.String.SLASH; // -1 display depth and empty tree display :"/"
        StringBuilder sb = new StringBuilder();
        for (UnsafeNode<T> root : this.root) {
            sb.append(Const.String.SLASH).append(Const.String.LF);
            recursiveBuildDisplayString(root, sb, Const.String.EMPTY, fnNodeDisplayFmt, true, displayDepth);
        }
        return sb.toString();
    }

    /**
     * 递归构造显示树的字符串
     *
     * @param node             节点
     * @param sb               StringBuilder
     * @param indent           缩进
     * @param fnNodeDisplayFmt 函数-表示node显示为字符串的形式
     * @param isLast           是否为最后一个（影响树打印效果）
     * @param displayDepth     要显示的截止深度（包含）
     */
    private void recursiveBuildDisplayString(UnsafeNode<T> node,
                                             StringBuilder sb,
                                             String indent,
                                             Function<? super UnsafeNode<T>, ? extends CharSequence> fnNodeDisplayFmt,
                                             boolean isLast,
                                             int displayDepth) {
        if (displayDepth < 0) return;
        sb.append(indent);
        if (isLast) {
            sb.append("└─ ");
            indent += "   ";
        } else {
            sb.append("├─ ");
            indent += "│  ";
        }
        // ANSI 转义 "红色下划线文本"
        // sb.append("\u001B[31;4m");
        sb.append(fnNodeDisplayFmt.apply(node));
        sb.append(Const.String.LF);
        // sb.append("\u001B[0m");
        if (node.isLeaf()) return;
        List<UnsafeNode<T>> children = node.unsafeGetChildNode();
        for (int i = 0; i < children.size(); i++) {
            recursiveBuildDisplayString(children.get(i), sb, indent,
                    fnNodeDisplayFmt,
                    i == children.size() - 1, displayDepth - 1);
        }
    }


    /**
     * 树的节点视图（node view）
     * <p>视图中没有修改Node及其树的方法，仅可查看Node中属性等
     *
     * @param <T> 节点数据类型 {@link MutNode#data()}
     * @apiNote 不要使用无法处理循环引用的方式将该类型直接序列化
     */
    public interface Node<T> {
        /**
         * 当前节点深度（边数计算法）
         *
         * @return 当前节点深度
         */
        int depth();

        /**
         * 当前节点载荷
         *
         * @return 节点载荷
         */
        T data();

        /**
         * 当前节点是否为叶子节点<br>
         * 叶子节点一定没有子节点
         *
         * @return true 是，反之则不是
         */
        boolean isLeaf();

        /**
         * 当前节点是否为根节点<br>
         * 根节点一定没有父节点
         *
         * @return true 是，反之则不是
         */
        default boolean isRoot() {
            return depth() == 0;
        }

        /**
         * 当前节点是否已从树中被剪掉
         *
         * @return true 是，反之则不是
         */
        boolean isPruned();

        /**
         * 返回当前节点的直接子节点个数
         *
         * @return 子节点个数
         */
        int childCount();

        /**
         * 返回当前节点父节点的 {@code Node.data()}<br>
         * 若没有父节点（根节点），抛出异常
         *
         * @return 父节点数据 node.data()
         * @throws NoSuchElementException 没有父节点（根节点）
         * @see #tryParentData()
         */
        T parentData() throws NoSuchElementException;

        /**
         * 返回当前节点父节点的 {@code Node.data()}<br>
         * 若没有父节点（根节点），返回 {@link Optional#empty()}
         *
         * @return Optional {@code Node.data()} | Optional.empty()
         */
        Optional<T> tryParentData();

        /**
         * 返回当前节点直接子节点的 {@code Node.data()}<br>
         * 若没有子节点，则返回 {@link Collections#emptyList()}<br>
         * 返回的 List 的类型是 {@code unmodifiableList}
         * <pre>
         *     {@code
         *     boolean isLeaf = node.isLeaf();
         *     if (isLeaf) {
         *         Assert.isTrue(node.child().isEmpty());
         *     } else {
         *         Assert.isTrue(node.child().size() > 0);
         *     }
         *     }
         * </pre>
         *
         * @return 直接子节点 {@code Node.data()}
         * @see #isLeaf()
         */
        List<T> childData();

        /**
         * 返回当前节点父节点<br>
         * 若没有父节点（根节点），抛出异常
         *
         * @return 父节点
         * @throws NoSuchElementException 没有父节点（根节点）
         */
        Node<T> parentNode() throws NoSuchElementException;


        /**
         * 当前节点父节点<br>
         * 若没有父节点（根节点），返回 {@link Optional#empty()}
         * <pre>
         *     {@code
         *     if (node.depth() == 0)
         *         Assert.eq(node.tryParentNode(), Optional.empty());
         *     }
         * </pre>
         *
         * @return Optional 父节点 | Optional.empty()
         * @see #depth()
         */
        Optional<Node<T>> tryParentNode();


        /**
         * 返回当前节点的直接子节点<br>
         * 若没有子节点，则返回 {@link Collections#emptyList()}<br>
         * 返回的 List 的类型是 {@code unmodifiableList}
         * <pre>
         *     {@code
         *     boolean isLeaf = node.isLeaf();
         *     if (isLeaf) {
         *         Assert.isTrue(node.childNode().isEmpty());
         *     } else {
         *         Assert.isTrue(node.childNode().size() > 0);
         *     }
         *     }
         * </pre>
         *
         * @return 直接子节点
         * @see #isLeaf()
         */
        List<Node<T>> childNode();

        /**
         * 将当前引用转为 {@link MutNode}
         * <pre>{@code
         * Node<T> node = ...;
         * if (conditionPruneNode){
         *     node.mut().prune();
         * }
         * if (conditionResetData){
         *     node.mut().resetData(...);
         * }
         * }</pre>
         *
         * @return Node（可变的）
         */
        default MutNode<T> mut() {
            return (MutNode<T>) this;
        }
    }


    /**
     * 树的节点（mutable node）<br>
     *
     * @param <T> 节点数据类型 {@link MutNode#data()}
     * @apiNote 不要使用无法处理循环引用的方式将该类型直接序列化
     * @see BidirectionalNode
     */
    public interface MutNode<T> extends Node<T> {

        /**
         * 将当前节点从树上剪掉
         * <p>进行该操作时，当前节点的所有子节点（包括间接子节点）都会从树中被剪掉
         * <p>该方法会修改树的节点状态</p>
         *
         * @throws IllegalStateException 对一个 {@code isPruned == true} 的节点再次调用
         * @apiNote 该方法返回 void，因为节点被删除后其便已经在某种意义上表示了“丢弃”，不会再返回该节点引用
         * @see #tryPrune()
         */
        void prune() throws IllegalStateException;

        /**
         * 将当前节点从树上剪掉，若已被剪掉，则直接返回
         * <p>进行该操作时，当前节点的所有子节点（包括间接子节点）都会从树中被剪掉
         * <p>该方法可能会修改树的节点状态</p>
         *
         * @apiNote 该方法返回 void，因为节点被删除后其便已经在某种意义上表示了“丢弃”，不会再返回该节点引用
         * @see #prune()
         */
        void tryPrune();

        /**
         * 将节点的data数据设定为给定实体
         *
         * @param data 新的数据
         * @return this
         * @throws NullPointerException 给定的实体为 {@code null}
         */
        MutNode<T> resetData(T data) throws NullPointerException;
    }

    /**
     * 树的节点（不安全的）
     * <p>相对于 {@link MutNode}，该类型引用可直接调用一系列 {@code unsafe} 方法，
     * 可直接修改树中节点的各项引用等，使用这些方法可能会造成树的节点状态信息与实际状态不一致
     *
     * @param <T> 节点载荷类型 {@code node.data()}
     * @apiNote 不要使用无法处理循环引用的方式将该类型直接序列化
     * @see BidirectionalNode
     */
    public interface UnsafeNode<T> extends MutNode<T> {

        @Override
        default List<Node<T>> childNode() {
            // unmodifiedList
            return isLeaf() ? Collections.emptyList() :
                    Collections.unmodifiableList(this.unsafeGetChildNode());
        }

        @Override
        default List<T> childData() {
            // unmodifiedList
            return isLeaf() ? Collections.emptyList() :
                    this.unsafeGetChildNode().stream().map(UnsafeNode::data).toList();
        }

        @Override
        default boolean isLeaf() {
            List<UnsafeNode<T>> childNodes = this.unsafeGetChildNode();
            return childNodes == null
                   || childNodes.isEmpty(); // 构造后默认不会有 empty的ChildNode，除非被变了
        }

        @Override
        default int childCount() {
            // maybe empty list
            List<UnsafeNode<T>> childNodes = this.unsafeGetChildNode();
            return childNodes == null ? 0 : childNodes.size();
        }

        @Override
        default boolean isPruned() {
            return unsafeGetHost() == null;
        }

        @Override
        default void prune() throws IllegalStateException {
            if (isPruned()) throw new IllegalStateException("node is pruned");
            Tree<T> t = this.unsafeGetHost();
            t.pruneNodeAndUpdateTreeState(this);
        }

        @Override
        default void tryPrune() {
            if (isPruned()) return;
            prune();
        }

        /**
         * 返回自身宿主树的直接引用， 可能为 {@code null}<br>
         * 若当前节点 {@code isPruned() == true},
         * 则一定返回 {@code null}
         *
         * @return hostTree | null
         */
        Tree<T> unsafeGetHost();

        /**
         * 修改自身宿主树的直接引用
         *
         * @apiNote 使用该方法可能会造成树的节点状态信息与实际状态不一致
         */
        void unsafeSetHost(Tree<T> host);

        /**
         * 返回父节点的直接引用，可能为 {@code null}<br>
         * 当前节点 {@code isRoot() == true}，
         * 则一定返回 {@code null}
         *
         * @return null | parentNode
         */
        UnsafeNode<T> unsafeGetParentNode();

        /**
         * 修改父节点引用<br>
         *
         * @apiNote 使用该方法可能会造成树的节点状态信息与实际状态不一致
         */
        void unsafeSetParentNode(UnsafeNode<T> parentNode);

        /**
         * 返回子节点列表的直接引用，可能为 {@code null}<br>
         * 返回的List类型为 {@link #listFactory} 提供的类型
         *
         * @return null | childNode
         */
        List<UnsafeNode<T>> unsafeGetChildNode();

        /**
         * 修改子节点引用
         *
         * @apiNote 使用该方法可能会造成树的节点状态信息与实际状态不一致
         */
        void unsafeSetChildNode(List<UnsafeNode<T>> childNode);

        /**
         * 修改当前节点深度
         *
         * @apiNote 使用该方法可能会造成树的节点状态信息与实际状态不一致
         */
        void unsafeSetDepth(int depth);

    }

    /**
     * 双向节点，内{@code parentNode}为对父节点的引用，
     * 作为Root的Node的parentNode一定为 {@code null}
     *
     * @param <T> 节点数据
     */
    static final class BidirectionalNode<T> implements MutNode<T>, UnsafeNode<T> {
        private Tree<T> host; // 宿主树，node 有其引用
        private T data;
        private int depth;
        private UnsafeNode<T> parentNode;
        private List<UnsafeNode<T>> childNodes;

        BidirectionalNode(Tree<T> host,
                          int depth,
                          T data,
                          UnsafeNode<T> parentNode) {
            this.host = host;
            this.depth = depth;
            this.data = data;
            this.parentNode = parentNode;
        }

        @Override
        public Tree<T> unsafeGetHost() {
            return host;
        }

        @Override
        public void unsafeSetHost(Tree<T> host) {
            // this can set owner to null
            this.host = host;
        }

        // no need impl equals & hashcode
        // 只有同属一颗树的，且父和子分别相同的才eq
        // 遂就是 Object.eq & hashcode 的语义

        @Override
        public String toString() {
            return Stf.f("BidirectionalNode(depth:{}, tree:{}){data: {}}", depth, host, data);
        }

        @Override
        public int depth() {
            return depth;
        }

        @Override
        public T data() {
            return data;
        }

        @Override
        public MutNode<T> resetData(T data) throws NullPointerException {
            this.data = Objects.requireNonNull(data, "given data is null");
            return this;
        }

        @Override
        public MutNode<T> parentNode() throws NoSuchElementException {
            if (parentNode == null) throw new NoSuchElementException("no parent node");
            return parentNode;
        }

        @Override
        public T parentData() throws NoSuchElementException {
            return parentNode().data();
        }

        @Override
        public Optional<Node<T>> tryParentNode() {
            // can't use Opt.of because root.parent is null
            return Optional.ofNullable(parentNode);
        }

        @Override
        public Optional<T> tryParentData() {
            return parentNode == null ? Optional.empty() : Optional.of(parentNode.data());
        }

        @Override
        public List<UnsafeNode<T>> unsafeGetChildNode() {
            return childNodes;
        }

        @Override
        public void unsafeSetChildNode(List<UnsafeNode<T>> childNode) {
            this.childNodes = childNode;
        }

        @Override
        public void unsafeSetDepth(int depth) {
            this.depth = depth;
        }

        @Override
        public UnsafeNode<T> unsafeGetParentNode() {
            return parentNode;
        }

        @Override
        public void unsafeSetParentNode(UnsafeNode<T> parentNode) {
            this.parentNode = parentNode;
        }

    }


    /**
     * tree的toString<br>
     * 在树过大时使用 {@link #toDisplayStr()} 并不好，遂仅显示基本信息
     *
     * @see #toDisplayStr()
     */
    @Override
    public String toString() {
        return Stf.f("Tree(root:{})[node: {}, depth: {}]@{}",
                rootCount(), nodeCount, depth, Integer.toHexString(this.hashCode()));
    }

    /**
     * 递归构造该Tree的Json形式<br>
     * 该方法主要为了方便将该Tree中各节点关系以Json形式构建字符串并传输，
     * 遂该方法返回的Json为压缩大小没有换行符及缩进等格式化输出形式
     * <pre>
     *     {@code
     *     record Obj(int id, String name) {};
     *     ObjectMapper mapper = new ObjectMapper(); // jackson
     *     Tree<Obj> emptyTree = Tree.empty();
     *     String emptyTreeJson = emptyTree.toJsonStr(Integer.MAX_VALUE,
     *             "objSelf",
     *             "objChild",
     *             node -> mapper.writeValueAsString(node.data()));
     *     Assertions.assertEquals("[]", emptyTreeJson);
     *     Tree<Obj> tree = Tree.ofLines(List.of(
     *                      Line.of(new Obj(1, "a"),new Obj(2, "b"))
     *                      )).unwrap();
     *     String json = tree.toJsonStr(tree.depth(),
     *             "objSelf",
     *             "objChild",
     *             node -> mapper.writeValueAsString(node.data()));
     *     System.out.println(json);
     *     // out(after format):
     *     // │[
     *     // │  {
     *     // │    "objSelf": {
     *     // │      "id": 1,
     *     // │      "name": "a"
     *     // │    },
     *     // │    "objChild": [
     *     // │      {
     *     // │        "objSelf": {
     *     // │          "id": 2,
     *     // │          "name": "b"
     *     // │        },
     *     // │        "objChild": []
     *     // │      }
     *     // │    ]
     *     // │  }
     *     // │]
     *     }
     * </pre>
     *
     * @param toJsonDepth         截止深度（包含）
     * @param nodeFieldName       节点在Json中的字段名
     * @param childArrayFieldName 节点的子节点数组在Json中的字段名
     * @param fnNode2Json         函数-会访问每个节点，描述一个node到Json字符序列的形式（通常只需要 {@code node.data()})
     * @return Json
     * @throws IllegalArgumentException 当给定的截止深度小于-1,nodeFieldName为空，childArrayFieldName为空时
     * @throws NullPointerException     给定的函数为空时
     * @apiNote 函数会访问每个节点，除非清楚在做什么，否则不应该在函数中通过node递归的访问其父或子
     */
    public String toJsonStr(int toJsonDepth,
                            String nodeFieldName,
                            String childArrayFieldName,
                            Function<? super Node<? super T>, ? extends CharSequence> fnNode2Json) {
        Objects.requireNonNull(fnNode2Json, "fnNode2Json is null");
        Err.realIf(nodeFieldName == null || nodeFieldName.isBlank(),
                IllegalArgumentException::new, "given node name is null or blank");
        Err.realIf(childArrayFieldName == null || childArrayFieldName.isBlank(),
                IllegalArgumentException::new, "given child list name is null or blank");
        Err.realIf(toJsonDepth < -1, IllegalArgumentException::new, "toJsonDepth < -1");
        if (toJsonDepth == -1 || this.isEmpty()) {
            return Const.String.ARRAY_EMPTY;
        }
        // 给定函数对每个元素执行
        StringBuilder sb = new StringBuilder();
        List<UnsafeNode<T>> nodes = this.root;
        sb.append(Const.Char.BRACKET_START);
        for (int i = 0, nodesSize = nodes.size(); i < nodesSize; i++) {
            UnsafeNode<T> rt = nodes.get(i);
            recursiveBuildJsonString(sb, rt, fnNode2Json, nodeFieldName,
                    childArrayFieldName, toJsonDepth);
            if (i < nodesSize - 1) {
                sb.append(Const.Char.COMMA);
            }
        }
        sb.append(Const.Char.BRACKET_END);
        return sb.toString();
    }

    /**
     * 递归构建json字符串
     *
     * @param sb                  stringBuilder
     * @param node                node
     * @param fnNode2Json         函数-将node转为 json的形式，由外界定义
     * @param nodeFieldName       节点字段名
     * @param childArrayFieldName 子节点数组字段名
     * @param toJsonDepth         深度（包含）
     */
    private void recursiveBuildJsonString(StringBuilder sb,
                                          UnsafeNode<T> node,
                                          Function<? super UnsafeNode<T>, ? extends CharSequence> fnNode2Json,
                                          String nodeFieldName,
                                          String childArrayFieldName,
                                          int toJsonDepth) {
        // { "node":
        sb.append(Const.Char.DELIM_START)
                .append(Const.Char.DOUBLE_QUOTES)
                .append(nodeFieldName)
                .append(Const.Char.DOUBLE_QUOTES)
                .append(Const.Char.COLON);
        // {...},
        sb.append(fnNode2Json.apply(node))
                .append(Const.Char.COMMA);
        // "child",[
        sb.append(Const.Char.DOUBLE_QUOTES);
        sb.append(childArrayFieldName);
        sb.append(Const.Char.DOUBLE_QUOTES);
        sb.append(Const.Char.COLON);
        sb.append(Const.Char.BRACKET_START);

        if (!node.isLeaf() && toJsonDepth - 1 >= 0) {
            List<UnsafeNode<T>> child = node.unsafeGetChildNode();
            for (int i = 0, childSize = child.size(); i < childSize; i++) {
                UnsafeNode<T> c = child.get(i);
                recursiveBuildJsonString(sb, c, fnNode2Json, nodeFieldName,
                        childArrayFieldName, toJsonDepth - 1);
                if (i < childSize - 1) {
                    sb.append(Const.Char.COMMA);
                }
            }
        }
        // ]}
        sb.append(Const.Char.BRACKET_END);
        sb.append(Const.Char.DELIM_END);
    }


    /**
     * 找给定节点从root到其parent的引用栈，弹栈 {@link Deque#poll()} 顺序为最近的parent...到root，
     * 当返回的栈为 empty 栈时，则证明给定的节点就是root其中之一，
     * 返回的栈是可写的栈 ArrayDeque
     *
     * @param node 需找一溜父节点的节点
     * @return 一溜父节点的栈，empty栈证明当前节点为root其一
     * @throws IllegalStateException 节点被删除了才调该方法找
     */
    private Deque<UnsafeNode<T>> findNodeAllParentRefReturnStack(UnsafeNode<T> node) {
        if (node.isPruned()) {
            throw new IllegalStateException("node is removed from tree");
        }
        int curDepth = node.depth();
        // 若为被删除的标志位，则证明外界持有该引用，但被删除了，或者，
        // 一个线程删掉了另一个线程调用该方法刷内存可见了
        // 若当前节点深度为0，则证明其为root之一，则其一溜父的栈必为empty，
        // 容量不用加一，因为当为当前节点深度时，不会加入到stack中，遂最多curDepth 个（索引0 到 curDep - 1)
        Deque<UnsafeNode<T>> dfsTempRouteStack = new ArrayDeque<>(curDepth);

        UnsafeNode<T> tempRef = node;
        while (!tempRef.isRoot()) {
            UnsafeNode<T> tempParent = tempRef.unsafeGetParentNode();
            dfsTempRouteStack.addLast(tempParent);
            tempRef = tempParent;
        }
        return dfsTempRouteStack;
    }

    /*
    node.genLine（路由）-- Tree.node(路由) -> Node 通过路由可以找到一个确定的Node（只要Tree没变（包括排序））
    MutNode<T> resetParentNode(Node<T> parent) throws IllegalArgumentException; 包括不同树间的node的reset
    后续若有机会或需求，应修改Node对外界返回的承载ChildNodes的List为可变的，
    且对List的修改，Tree应该立即可见，这也许并不容易，因为即使使用下List将存储ChildNodes的List
    代理，使之能够响应对应add或remove的操作，但List吐出去的 Iterator 呢？吐出去的Stream呢？
    这些额外的实体的行为都需要做细细考究才能决策及定义其语义
    private static class SafeModifyTreeProxyList<T> implements List<T> {
        private final Tree<T> treeRef;
        private final Node<T> self; // if root, this is null
        private final List<T> cNodes;
    }
    */


}
