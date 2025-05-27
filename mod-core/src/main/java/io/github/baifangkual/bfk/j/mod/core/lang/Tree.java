package io.github.baifangkual.bfk.j.mod.core.lang;


import io.github.baifangkual.bfk.j.mod.core.mark.Iter;
import io.github.baifangkual.bfk.j.mod.core.panic.Err;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * <b>Tree</b><br>
 * 多叉树，通用树，状态可变，线程不安全<br>
 * 该树的深度 {@link #depth} 使用边数计算法（空树（{@link #roots} is empty）的深度 = -1，只有根节点的树的深度 = 0）<br>
 * 因为该树为二叉树的父集，遂没有中序遍历<br>
 *
 * @author baifangkual
 * @since 2023/8/18 v0.0.6
 */
public final class Tree<T> implements Iter<T> {
    // todo tree doc
    // ref emptyTree
    private static final Tree<?> EMPTY_TREE_IMMUTABLE = new Tree<>(); // emptyTree，immutable
    private static final Consumer<Object> FN_ACC_DO_NOTHING = (n) -> { /*do nothing...*/};

    // immutable
    private final Supplier<? extends List<Node<T>>> listFactory; // 各node中childNodes类型
    private final FnChildSet<T> fnSetChild;
    private final FnUnsafeGetChild<T> fnUnsafeGetChild;
    private final FnRefNodeConstructor<T> fnNewNode;
    private final NodeType nodeType;
    private final List<Node<T>> roots;
    private final BiConsumer<Node<T>, Node<T>> fnSetParentNode;
    private final Consumer<Node<T>> fnDestroyNode;

    // ------- mutable
    private int nodeCount = 0;
    private int depth = -1;
    private int modifyCount = 0; // 用以验证是否有同时使用tree的临时视图（如迭代器）对树做修改
    // ------- mutable


    @SuppressWarnings("unchecked")
    public static <T> Tree<T> empty() {
        return (Tree<T>) EMPTY_TREE_IMMUTABLE;
    }

    public boolean isEmpty() {
        return nodeCount == 0;
    }

    /**
     * 根节点<br>
     * 返回的 List 的类型是构造树时使用的 {@link #listFactory} 提供的类型
     * // todo modify view
     *
     * @return 根节点
     */
    public List<Node<T>> roots() {
        return roots;
    }

    /**
     * 根节点个数
     *
     * @return 根节点个数
     */
    public int rootCount() {
        return roots.size();
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
     * 树节点类型
     *
     * @return 树节点类型
     */
    public NodeType nodeType() {
        return nodeType;
    }

    /**
     * 空树，不可变，不可添加节点
     */
    private Tree() {
        this(Collections.emptyList(),
                n -> null,
                NodeType.unidirectionalNode,
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
     * @param fnGetChild         函数，要求给定一个实体，返回这个实体的子实体，返回的迭代器可以为null，
     *                           也可以没有元素<b>(该函数仅在Tree构造时使用，Tree构造完成便被丢弃）</b>
     * @param nodeType           树中节点使用什么类型的实现（{@link BidirectionalNode} or {@link UnidirectionalNode}）
     * @param fnSort             函数-实体排序的函数<b>(该函数仅在Tree构造时使用，Tree构造完成便被丢弃）</b>
     * @param listFactory        函数-List构造方法引用<b>(该函数仅在Tree构造时使用，Tree构造完成便被丢弃）</b>
     * @param fnPreCheckHasChild 函数(nullable)，要求给定一个实体，返回布尔值标识该实体是否有子，
     *                           这在lsChildFn过重时是一种优化<b>(该函数仅在Tree构造时使用，Tree构造完成便被丢弃）</b>
     * @param fnPreFilter        函数，预先处理树中实体，要求给定一个实体，当返回的布尔值为false时，
     *                           表示实体不加入树<b>(该函数仅在Tree构造时使用，Tree构造完成便被丢弃）</b>
     * @param maxDepth           最大停止深度（包含），
     *                           表示该树所承载的最大层级，当已达到树所承载的最大层级，即使树的叶子节点仍可获取子，也不会进行该操作
     * @throws NullPointerException     当不允许为空的参数给定空时
     * @throws IllegalArgumentException 当给定的最大停止深度小于 -1 时
     */
    Tree(Iterable<? extends T> roots,
         Function<? super T, ? extends Iterable<T>> fnGetChild,
         NodeType nodeType,
         Comparator<? super T> fnSort,
         Supplier<? extends List<Node<T>>> listFactory,
         Predicate<? super T> fnPreCheckHasChild,
         Predicate<? super T> fnPreFilter,
         int maxDepth) {

        // null check （fnPreCheck is nullable, no need check）
        Objects.requireNonNull(fnSort, "fnSort is null");
        Objects.requireNonNull(nodeType, "nodeType is null");
        Objects.requireNonNull(roots, "roots is null");
        Objects.requireNonNull(listFactory, "listFactory is null");
        Objects.requireNonNull(fnGetChild, "fnGetChild is null");
        Objects.requireNonNull(fnPreFilter, "fnPreFilter is null");

        // given maxDepth < -1 Illegal
        Err.realIf(maxDepth < -1, IllegalArgumentException::new, "maxDepth < -1");

        // args 2 node
        this.fnNewNode = switchNBuildFn(nodeType);
        // node set child
        this.fnSetChild = switchNCSetFn(nodeType);
        this.fnUnsafeGetChild = switchUnsafeGetChildFn(nodeType);
        this.nodeType = nodeType;
        this.listFactory = listFactory;
        this.fnSetParentNode = switchSetParentNodeFn(nodeType);
        this.fnDestroyNode = switchDestroyNodeFn(nodeType);

        // compose fnHasChild and fnGetChild
        final Function<? super T, ? extends Iterable<T>> fnCompGetChild =
                compGetChildFn(fnPreCheckHasChild, fnGetChild);
        // node comp
        final Comparator<Node<T>> fnNSort = compNSortFn(fnSort);
        // 需隔绝外界给定的roots的Iter
        // 临时List存储root，root通常不多
        List<Node<T>> rootCollector = listFactory.get();
        // roots build
        Iter.toStream(roots).filter(fnPreFilter)
                .map(t -> this.fnNewNode.newNode(this, 0, t, null))
                .sorted(fnNSort)
                .forEach(rootCollector::add);

        // 允许没有元素的树，若是，则无需下构建树子
        if (maxDepth == -1 || rootCollector.isEmpty()) {
            rootCollector.clear(); // 因为内部的List由ListFactory控制，遂该即使为空树，roots也不应引用Collections.emptyList
            this.roots = rootCollector;
        } else {
            this.nodeCount = rootCollector.size();
            buildingTree(rootCollector, listFactory, fnCompGetChild, fnPreFilter,
                    listFactory.get(), // 复用临时存储直接child的，通常直接child不会太多
                    fnNSort, maxDepth - 1, 0);
            this.roots = rootCollector;
        }

    }

    public static <E> Tree<E> ofRoots(Iterable<? extends E> roots,
                                      Function<? super E, ? extends Iterable<E>> fnGetChild,
                                      NodeType type,
                                      Comparator<? super E> fnSort,
                                      Supplier<? extends List<Node<E>>> listFactory,
                                      Predicate<? super E> fnPreCheckHasChild,
                                      Predicate<? super E> fnPreFilter,
                                      int maxDepth) {
        return new Tree<>(roots, fnGetChild, type, fnSort, listFactory, fnPreCheckHasChild, fnPreFilter, maxDepth);
    }


    public static <E> Tree<E> ofRoots(Iterable<? extends E> roots,
                                      Function<? super E, ? extends Iterable<E>> fnGetChild,
                                      NodeType type,
                                      Supplier<? extends List<Node<E>>> listFactory) {
        final Predicate<? super E> defaultFnPre = (e) -> true;
        final Comparator<? super E> defaultFnSort = Comparator.nullsFirst(null);
        return Tree.ofRoots(roots, fnGetChild, type, defaultFnSort, listFactory, defaultFnPre, defaultFnPre, Integer.MAX_VALUE);
    }

    public static <E> Tree<E> ofRoots(Iterable<? extends E> roots,
                                      Function<? super E, ? extends Iterable<E>> fnGetChild) {
        return Tree.ofRoots(roots, fnGetChild, NodeType.unidirectionalNode, ArrayList::new);
    }

    /**
     * 从线段关系构建树<br>
     * 若线段关系可以构建成树，返回{@link R.Ok}载荷{@link Tree}，
     * 否则返回{@link R.Err}载荷构建树过程发生的异常{@link RuntimeException}<br>
     * 若给定的lines中至少有一个线段，则一定有:
     * <pre>
     *     {@code
     *     List<Line<E>> inLines = ...;
     *     Tree<E> tree = Tree.tryOfLines(inLines).unwrap();
     *     List<Line<E>> outLines = tree.tryToLines(LinkedList::new, Tree.Node::data).unwrap();
     *     Comparator<Line<E>> lineSort = ...;
     *     List<Line<E>> sortInLines = inLines.stream().sorted(lineSort).toList();
     *     List<Line<E>> sortOutLines = outLines.stream().sorted(lineSort).toList();
     *     Assert.eq(sortInLines, sortOutLines);
     *     }
     * </pre>
     *
     * @param lines n个线段
     * @param <E>   有效载荷
     * @return Ok(Tree) | Err(...)
     * @apiNote 若给定的lines为空、其中没有任何元素、线段构成的关系包含循环边、
     * 线段中有实体有一个以上父节点等，返回{@link R.Err}
     * @see #tryToLines(Supplier, Function)
     */
    public static <E> R<Tree<E>> tryOfLines(Iterable<Line<E>> lines) {
        return R.ofFnCallable(() -> {
            List<Line<E>> allLines = Iter.toStream(lines).toList();
            boolean isTree = Line.isTree(allLines);
            if (!isTree) throw new IllegalArgumentException("Not a tree: " + allLines);
            Set<E> headerNodes = Line.findHeaderNodes(allLines);
            Map<E, List<E>> selfRefChildList = allLines.stream()
                    .collect(Collectors.groupingBy(
                            Line::begin,
                            Collectors.mapping(Line::end, Collectors.toList())
                    ));
            return Tree.ofRoots(headerNodes, selfRefChildList::get);
        });
    }

    /**
     * 将树中的节点关系用线段表达，返回n个线段<br>
     * 若树中关系可以以n个{@link Line}形式表达，则返回{@link R.Ok}载荷n个Line，
     * 否则返回{@link R.Err}载荷执行过程发生的异常{@link RuntimeException}<br>
     * 若树为空树({@code tree.count == 0 && tree.depth == -1})，则返回的List为空集合(emptyList)<br>
     *
     * @param listFactory 函数-List构造方法引用，表示返回的承载n个Line的List实现类
     * @param fn          函数-能够接收一个{@link Node}，返回一个{@link V}，映射函数
     * @param <V>         通过函数转换的Line中承载的类型
     * @return Ok(Lines) | Err(...)
     * @apiNote 仅包含根节点的树（因为没有子节点，无法构造{@link Line})、
     * 给定的listFactory为空、给定的fn为空等，均会返回{@link R.Err}
     * @see #tryOfLines(Iterable)
     */
    public <V> R<List<Line<V>>> tryToLines(Supplier<? extends List<Line<V>>> listFactory,
                                           Function<? super Node<T>, ? extends V> fn) {
        return R.ofFnCallable(() -> {
            final List<Line<V>> r = listFactory.get();// get list type link? arr?
            final Queue<Node<T>> queue = new LinkedList<>(); //BFS queue
            for (Node<T> root : this.roots) { // 先将root判定，添加到queue中
                if (root.isLeaf()) throw new IllegalStateException("tree just root node, can't build Line.end");
                queue.add(root);
            }
            while (!queue.isEmpty()) {
                Node<T> n = queue.poll();
                // 因为queue中都是有叶子节点的，遂这里不用判定
                List<Node<T>> childNodes = this.fnUnsafeGetChild.getNullableChild(n);
                for (Node<T> child : childNodes) {
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
     * 返回一个迭代器<br>
     * 返回的迭代器是 <i>fail-fast</i> 的，
     * 可使用该迭代器以 BFS 方式从 {@link #roots} 开始迭代该树中的所有节点<br>
     * 这个迭代器在调用 {@link Iterator#remove()} 方法时将会从树中删除节点及其自身的所有子节点，
     * 即一次 {@code remove()} 操作可能会删除 n 个元素（若当前节点为叶子节点，则仅会删除一个元素（自己），
     * 若非叶子节点，则会删除大于1个元素（自己及自己的子级））<br>
     *
     * @return 树的迭代器
     * @apiNote 该迭代器并非线程安全，且使用该迭代器对树中元素进行修改将会立即在树中生效并可见<br>
     * 一次树的迭代需求应当仅使用一个迭代器，不应同时持有一个树的多个迭代器，若同时持有一个树的多个迭代器，
     * 在使用一个迭代器对树进行修改时（比如调用 {@code remove()})，
     * 其他迭代器可能已经迭代到需被修改的节点的子级，若这种情况发生，
     * 则再次使用其他迭代器时将抛出 {@link ConcurrentModificationException}<br>
     * 若同一时间始终使用唯一的一个修改树的迭代器进行迭代，则不会抛出 {@link ConcurrentModificationException}<br>
     * @see ConcurrentModificationException
     * @see #iterator()
     */
    public Iterator<Node<T>> nodeIterator() {
        return new TreeIter<>(this);
    }

    /**
     * 返回一个迭代器<br>
     * 返回的迭代器是 <i>fail-fast</i> 的，
     * 可使用该迭代器以 BFS 方式从 {@link #roots} 开始迭代该树中的所有元素<br>
     * 这个迭代器在调用 {@link Iterator#remove()} 方法时将会从树中删除节点及其自身的所有子节点，
     * 即一次 {@code remove()} 操作可能会删除 n 个元素（若当前节点为叶子节点，则仅会删除一个元素（自己），
     * 若非叶子节点，则会删除大于1个元素（自己及自己的子级））<br>
     *
     * @return 树的迭代器
     * @apiNote 该迭代器并非线程安全，且使用该迭代器对树中元素进行修改将会立即在树中生效并可见<br>
     * 一次树的迭代需求应当仅使用一个迭代器，不应同时持有一个树的多个迭代器，若同时持有一个树的多个迭代器，
     * 在使用一个迭代器对树进行修改时（比如调用 {@code remove()})，
     * 其他迭代器可能已经迭代到需被修改的节点的子级，若这种情况发生，
     * 则再次使用其他迭代器时将抛出 {@link ConcurrentModificationException}<br>
     * 若同一时间始终使用唯一的一个修改树的迭代器进行迭代，则不会抛出 {@link ConcurrentModificationException}<br>
     * @see ConcurrentModificationException
     * @see #nodeIterator()
     */
    @SuppressWarnings("NullableProblems")
    public Iterator<T> iterator() {
        return ETMProxyIter.of(new TreeIter<>(this), Node::data);
    }

    /**
     * 树的迭代器<br>
     * BFS 方式，内有 Queue 作为对当前层节点的子节点的缓存，
     * Queue 中还有作为 fence（栅栏）的标记节点，该节点外界不可见，
     * fence 仅是为了隔离不同父节点的一批节点
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
        Node<E> newFence(List<Node<E>> parentNodeChildNodesListRef, Node<E> parentNodeRef) {
            // 能调用到该方法，则该参数的List一定不为null并且不为empty
            BidirectionalNode<E> fenceNode = new BidirectionalNode<>(null, FENCE_DEPTH, null, parentNodeRef);
            fenceNode.childNodes = parentNodeChildNodesListRef;
            return fenceNode;
        }

        /**
         * 测试 node 是否为 栅栏
         */
        boolean isFence(Node<E> node) {
            return node.depth() == FENCE_DEPTH;
        }

        final Tree<E> treeRef;
        final Queue<Node<E>> queue = new LinkedList<>(); // 滚动

        // modifyFlag 用以验证是否有同时使用多个迭代器对一个树做修改
        int modifyCount;

        Node<E> currentParentNodeRef; // fence后节点的父节点引用
        Node<E> beforeNodeRef; // 前一个node引用
        List<Node<E>> currentParentChildNodesRef; // 当前的node的父级的childNodesList的引用
        List<Node<E>> beforeNodeChildNodesRef; // 上一个node可能为叶子节点 , 所以该，可能为null

        Node<E> current; // 当前被next的node的引用，用以从currentParentChildNodesRef中remove时使用

        TreeIter(Tree<E> tree) {
            List<Node<E>> roots = tree.roots;
            this.treeRef = tree;
            this.currentParentChildNodesRef = roots;
            this.queue.addAll(roots);
            // 初始化
            this.beforeNodeRef = null;
            this.currentParentNodeRef = null; //root no parent
            this.modifyCount = tree.modifyCount;
            // root 添加后不必添加Fence，因为root的currentParentChildNodesRef已经有引用了
        }

        /**
         * 若修改计数与树的对不上，抛异常
         */
        final void assertJustMeModifyTree() {
            if (treeRef.modifyCount != this.modifyCount) {
                throw new ConcurrentModificationException("Tree 已被其他实体修改, 当前迭代器已迭代到被删除的节点");
            }
        }

        @Override
        public boolean hasNext() {
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
                Node<E> fenceNode = newFence(beforeNodeChildNodesRef, beforeNodeRef);
                queue.add(fenceNode);
                queue.addAll(beforeNodeChildNodesRef);
                beforeNodeChildNodesRef = null;
                beforeNodeRef = null;
            }


            // 可能取出来的是标记对象，这里应当用remove？ no！因为还有current引用，若这里抛出异常
            // 则调用remove时会将未被重置的current（也就是上一个）执行删除，这会破坏迭代器的remove语义
            // ---- 这里也可用remove，只需再iter的该next方法头将current置位null即可
            Node<E> nodeOrFence = queue.poll();
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
            Node<E> n;

            // 因为该方法之前会将beforeNodeChildNodesRef引用中的node都添加到队列，若队列为空，则证明
            // 当前node的任何一个上一个node都没有子，也就是树已经走完了
            if (isFence(nodeOrFence)) {  // 若是栅栏，则切换自己父的引用
                // 因为已经判定了是fenceNode，所以一定为UnidirectionalNode，并且其持有的
                // childNodes 就是队列中当前fenceNode之后，下一个fenceNode之前的所有node的父引用的CNRef
                // UnidirectionalNode已修改为BidirectionalNode,因为要持有父的引用
                currentParentChildNodesRef = ((BidirectionalNode<E>) nodeOrFence).childNodes;
                currentParentNodeRef = ((BidirectionalNode<E>) nodeOrFence).parentNode;

                // 20250527 因为 node 拥有了 destroy 情况，
                // 遂可能其他迭代器已经删除其或其父，导致其自身也被标记删除，
                // 但仍在当前迭代器缓存队列中，遂若为 destroy 的，则丢弃引用并重新获取
                // 若直到最后都没找到一个可用的，说明已经遍历完成了？
                // 不可，因为hasNext为true，就应当返回一个元素，而不是抱有侥幸心理行事
                // 当 发现destroy，应当立即抛出异常

                n = queue.poll(); // 若是标记对象，再取一个 , 根据逻辑，一定不会有两个fence连着
                //noinspection DataFlowIssue
                if (isDestroy(n)) {
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
                beforeNodeChildNodesRef = treeRef.fnUnsafeGetChild.getNullableChild(n); // 获取childNodes的直接引用
                beforeNodeRef = n;
            }
            current = n; // iter内暂存被next的node的引用，用以删除时使用
            return n;
        }

        @Override
        public void remove() {
            // remove当前被next迭代到的node的意义就是：
            // 从当前node的父节点的CNRef中删除自己
            // 无需判定是否为双引用节点并将自身的parentNode引用置位null，
            // 因为自己持有自己父的引用不会阻止垃圾回收的GCRoot可达性判定
            // 为help GC，后续或可优化，Iter拿得到当前的nodeType，当为BINode双向引用时
            // 且不为root时，可选择清除对父的引用
            if (current != null) {
                assertJustMeModifyTree();
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
     * 剪枝，从父的子中移除，并且更新树的状态值<br>
     * 该方法不会有{@link #pruneNodeAndUpdateTreeState(Node, Node, List, boolean)} 方法API描述的问题
     *
     * @param dumpNode   要剪掉的node
     * @param parentNode 要dump的node的父节点
     * @throws NullPointerException  parentNode is null
     * @throws IllegalStateException parentNode is leaf
     */
    private void pruneNodeAndUpdateTreeState(Node<T> dumpNode, Node<T> parentNode) {
        Objects.requireNonNull(parentNode, "given parentNode is null");
        if (parentNode.isLeaf()) {
            throw new IllegalStateException("given parentNode is leaf");
        } else {
            pruneNodeAndUpdateTreeState(dumpNode, parentNode,
                    this.fnUnsafeGetChild.getNullableChild(parentNode), true);
        }
    }


    /**
     * 剪枝，从父的子中移除，并且更新树的状态值
     *
     * @param dumpNode                   要剪掉的node
     * @param nullableParentNodeRef      要dump的node的父节点（nullable（当为root节点时））
     * @param parentChildNodesListRef    父的子集合
     * @param ifPossibleDumpEmptyListRef 选项，若为true，则如果可以，当 dumpNode 被丢弃后，
     *                                   被丢弃的node的父的子List为empty时，将该List引用丢弃，
     *                                   （无论如何，该不会丢弃roots所在的List的引用）
     * @throws NullPointerException     parentChildNodesListRef is null
     * @throws IllegalStateException    parentChildNodesListRef not found dumpNode ref
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
            Node<T> dumpNode,
            Node<T> nullableParentNodeRef,
            List<Node<T>> parentChildNodesListRef,
            boolean ifPossibleDumpEmptyListRef
    ) {
        Objects.requireNonNull(parentChildNodesListRef);
        if (!parentChildNodesListRef.contains(dumpNode)) {
            // 若父的子集合中无当前，则直接用异常中断后续逻辑，防止更新树全局变量
            // fix：还有一点，若同一时间持有树的两个迭代器，则第二个迭代器在进行删除操作时
            //   可能 需被删除的节点 已经被删除（因为被第一个迭代器删除的节点可能已存在于
            //   第二个迭代器的 BFS 缓存队列中，这时，被删除的节点将在第二个迭代器上可见），
            //   若未有该层校验，则可能造成树的状态值异常刷新 eg treeDepth and nodeCount...
            throw new IllegalStateException("prune node.parentChildNodes not fount he self");
        }
        // 使用两个变量记录被剪枝的node上的count和depth，用以更新Tree的count和depth
        final int[] countAndDeepLeafDepth = {0, 0};
        // 从要删除的node bfs 遍历 局部bfs
        bfs(() -> List.of(dumpNode),
                n -> {
                    countAndDeepLeafDepth[0] = countAndDeepLeafDepth[0] + 1; //count ++
                    // 因为为bfs，所以最后的点一定深度最深，所以无需Math.max比较
                    countAndDeepLeafDepth[1] = n.depth(); // until leaf depth update
                },
                FN_ACC_DO_NOTHING,
                fnDestroyNode);
        // 完成后 里面就有 count 和 最深叶子节点的depth了
        this.nodeCount -= countAndDeepLeafDepth[0]; //tree nodeCount 更新
        int deepLeafDepth = countAndDeepLeafDepth[1];
        // 删除自身引用
        parentChildNodesListRef.remove(dumpNode);
        // 将树的变更记录 + 1
        this.modifyCount += 1;

        // 优化：若parentChildNodes已经为empty（即被丢弃的node是其唯一的子），则丢弃空集合引用
        // 这里拿不到parentNode ... 做不到，下面有bfs，可以下面拿...

        boolean emptyListRefWasDump = false;
        if (nullableParentNodeRef != null // 这里无需判定是否为root的List，因为root没有parentNode
            && ifPossibleDumpEmptyListRef
            && parentChildNodesListRef.isEmpty()) {
            if (this.fnUnsafeGetChild.getNullableChild(nullableParentNodeRef) == parentChildNodesListRef) {
                // 若不是父的子List，抛出异常，否将引用置位null
                this.fnSetChild.set(nullableParentNodeRef, null);
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
                && parentChildNodesListRef != this.roots) {
                // 要获取直接的ChildNodes引用不能用isLeaf判定，因为其会把emptyList忽略
                // 因为下直接获取ChildNodes引用，遂可能不为null而为empty，这时就不能在nullableChild == null块内更新depth了
                this.bfs(n -> {
                    countAndDeepLeafDepth[0] = n.depth();
                    // to do unIsLeaf，因为空集合也会被判定为Leaf，所以应当直接获取其引用
                    // 因为parentChildNodes这时候不可能等于null，遂可以直接判断是否相等 ==
                    if (parentChildNodesListRef == this.fnUnsafeGetChild.getNullableChild(n)) {
                        // 若找到了自己的父级 且为 empty
                        this.fnSetChild.set(n, null); // 丢弃引用
                    }
                });
                // 否则，仅计算新的depth
            } else {
                // 因为为bfs，所以最后的点一定深度最深，所以无需Math.max比较
                this.bfs(n -> countAndDeepLeafDepth[0] = n.depth());
            }
            // 更新树深
            this.depth = countAndDeepLeafDepth[0];
        }
    }


    /**
     * 递归-DFS<br>
     * 根据给定的函数操作，可表示先序或后序遍历
     *
     * @param node      树节点
     * @param fnPreAcc  函数-先序遍历访问
     * @param fnPostAcc 函数-后序遍历访问
     * @throws NullPointerException 给定的节点为空或函数为空时
     */
    private void dfs(Node<T> node,
                     Consumer<? super Node<T>> fnPreAcc,
                     Consumer<? super Node<T>> fnPostAcc) {
        fnPreAcc.accept(node); // 先访问
        if (!node.isLeaf()) {
            List<Node<T>> cList = this.fnUnsafeGetChild.getNullableChild(node);
            for (Node<T> child : cList) {
                dfs(child, fnPreAcc, fnPostAcc);// 递归子
            }
        }
        fnPostAcc.accept(node); // 后访问
    }

    /**
     * 非递归-BFS遍历
     *
     * @param fnGetNodes 树节点提供函数，提供n个树节点，这些节点应是同一深度的，
     *                   否则可能会导致一个实体被遍历不止一次
     * @param fn         函数
     * @throws NullPointerException 给定的节点为空或函数为空时
     */
    private void bfs(Supplier<? extends Iterable<Node<T>>> fnGetNodes,
                     Consumer<? super Node<T>> fn) {
        this.bfs(fnGetNodes, fn, FN_ACC_DO_NOTHING, FN_ACC_DO_NOTHING);
    }

    /**
     * 非递归-BFS遍历
     *
     * @param fnGetNodes              树节点提供函数，提供n个树节点，这些节点应是同一深度的，
     *                                否则可能会导致一个实体被遍历不止一次
     * @param fnPreAcc                函数，一定对node执行
     * @param fnAfterChildAddQueueAcc 函数，仅对有子节点的node执行，在被遍历的node的子添加到bfs队列后执行
     * @param fnPostAcc               函数，一定对node执行
     * @throws NullPointerException 给定的节点为空或函数为空时
     */
    private void bfs(Supplier<? extends Iterable<Node<T>>> fnGetNodes,
                     Consumer<? super Node<T>> fnPreAcc,
                     Consumer<? super Node<T>> fnAfterChildAddQueueAcc,
                     Consumer<? super Node<T>> fnPostAcc) {
        Objects.requireNonNull(fnGetNodes, "fnGetNodes is null");
        Objects.requireNonNull(fnPreAcc, "fnPreAcc is null");
        Objects.requireNonNull(fnPostAcc, "fnPostAcc is null");
        Objects.requireNonNull(fnAfterChildAddQueueAcc, "fnAfterChildAddQueueAcc is null");
        Queue<Node<T>> queue = new LinkedList<>();
        fnGetNodes.get().forEach(queue::add);
        while (!queue.isEmpty()) {
            Node<T> current = queue.poll();
            fnPreAcc.accept(current); // pre 处理当前节点
            // 将当前节点的所有子节点加入队列
            if (!current.isLeaf()) {
                queue.addAll(this.fnUnsafeGetChild.getNullableChild(current));
                fnAfterChildAddQueueAcc.accept(current);
            }
            fnPostAcc.accept(current); // 最后对current执行
        }
    }

    /**
     * 非递归-BFS遍历
     *
     * @param fn 函数-访问每个被遍历到的{@link Node}
     * @throws NullPointerException 给定的节点为空或函数为空时
     */
    public void bfs(Consumer<? super Node<T>> fn) {
        bfs(() -> roots, fn);
    }

    /**
     * 递归-DFS先序遍历
     *
     * @param fn 函数-访问每个被遍历到的{@link Node}
     * @throws NullPointerException 给定的节点为空或函数为空时
     */
    public void dfsPreOrder(Consumer<? super Node<T>> fn) {

        for (Node<T> root : roots) {
            dfs(root, fn, FN_ACC_DO_NOTHING);
        }
    }

    /**
     * 递归-DFS后序遍历
     *
     * @param fn 函数-访问每个被遍历到的{@link Node}
     * @throws NullPointerException 给定的节点为空或函数为空时
     */
    public void dfsPostOrder(Consumer<? super Node<T>> fn) {
        for (Node<T> root : roots) {
            dfs(root, FN_ACC_DO_NOTHING, fn);
        }
    }

    // DEF FUNCTION ===========================

    /**
     * compose nullable hasChildFn and getChildFn
     */
    private Function<? super T, ? extends Iterable<T>>
    compGetChildFn(Predicate<? super T> nullableFnHasChild,
                   Function<? super T, ? extends Iterable<T>> fnGetChild) {
        return nullableFnHasChild == null ? fnGetChild : (e) -> nullableFnHasChild.test(e) ? fnGetChild.apply(e) : null;
    }

    /**
     * Node impl constructor ref
     */
    @FunctionalInterface
    private interface FnRefNodeConstructor<T> {
        Node<T> newNode(Tree<T> owner, int dep, T vRef, Node<T> pRef);
    }

    /**
     * gen node new fn by node type
     */
    private FnRefNodeConstructor<T>
    switchNBuildFn(NodeType type) {
        return switch (type) {
            case bidirectionalNode -> BidirectionalNode::new;
            case unidirectionalNode -> UnidirectionalNode::new;
        };
    }

    /**
     * node child setter
     */
    @FunctionalInterface
    private interface FnChildSet<T> {
        void set(Node<T> self, List<Node<T>> child);
    }

    /**
     * gen node child set fn by node type
     */
    private FnChildSet<T>
    switchNCSetFn(NodeType type) {
        // JIT check cast
        return switch (type) {
            case bidirectionalNode -> (self, child) ->
                    ((BidirectionalNode<T>) self).childNodes = child;
            case unidirectionalNode -> (self, child) ->
                    ((UnidirectionalNode<T>) self).childNodes = child;
        };
    }

    /**
     * node child getter
     */
    @FunctionalInterface
    private interface FnUnsafeGetChild<T> {
        List<Node<T>> getNullableChild(Node<T> n);
    }

    /**
     * gen unsafe getChild fn
     */
    private FnUnsafeGetChild<T>
    switchUnsafeGetChildFn(NodeType type) {
        // JIT check cast
        return switch (type) {
            case unidirectionalNode -> self -> ((UnidirectionalNode<T>) self).childNodes;
            case bidirectionalNode -> self -> ((BidirectionalNode<T>) self).childNodes;
        };
    }

    /**
     * 将 比较T类型的 函数 转为 比较Node[T]类型的 函数
     */
    private Comparator<Node<T>>
    compNSortFn(Comparator<? super T> sort) {
        return (n1, n2) -> sort.compare(n1.data(), n2.data());
    }

    /**
     * 构造设置parentNode的函数，（unidirectionalNode什么都不做）
     */
    private BiConsumer<Node<T>, Node<T>>
    switchSetParentNodeFn(NodeType type) {
        return switch (type) {
            case bidirectionalNode -> (self, p) -> ((BidirectionalNode<T>) self).parentNode = p;
            case unidirectionalNode -> (self, p) -> {/* do nothing*/};
        };
    }

    /**
     * 被标记为已破坏的node的depth，可以以此判断是否已破坏,
     * 因为 node 的 depth 正常情况不会为 -1，遂可以以该值做标志位判定
     */
    private static final int DESTROY_DEPTH = -1;

    /**
     * 判定node是否处于 已破坏 状态
     */
    private static boolean isDestroy(Node<?> node) {
        return node.depth() == DESTROY_DEPTH;
    }

    /**
     * 销毁/破坏 node，将其所有引用清除，能断就断，并将depth置位{@value #DESTROY_DEPTH}
     */
    private Consumer<Node<T>>
    switchDestroyNodeFn(NodeType type) {
        return switch (type) {
            case bidirectionalNode -> (n) -> {
                BidirectionalNode<T> node = (BidirectionalNode<T>) n;
                node.parentNode = null;
                node.data = null;
                node.childNodes = null; // 不调用CNs的clear方法，因为其可能为空
                node.depth = DESTROY_DEPTH;
            };
            case unidirectionalNode -> (n) -> {
                UnidirectionalNode<T> node = (UnidirectionalNode<T>) n;
                node.data = null;
                node.childNodes = null; // 不调用CNs的clear方法，因为其可能为空
                node.depth = DESTROY_DEPTH;
            };
        };
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
    private void buildingTree(List<Node<T>> nodes,
                              Supplier<? extends List<Node<T>>> listFactory,
                              Function<? super T, ? extends Iterable<? extends T>> fnGetChild,
                              Predicate<? super T> fnFilterTest,
                              List<Node<T>> tempNodeRefReusableList,
                              Comparator<Node<T>> fnNSort,
                              int maxDepth,
                              int currentDepth) {
        // 到达最大层 直接返回
        this.depth = Math.max(this.depth, currentDepth);
        if (maxDepth < 0) return;
        // 已被fnFilterTest走过的node
        for (Node<T> node : nodes) {
            T ref = node.data();
            Iterable<? extends T> it = fnGetChild.apply(ref);
            if (it != null) {
                // 清理临时持有Node引用的集合
                tempNodeRefReusableList.clear();
                for (T child : it) {
                    // 若元素未能通过test，则不将其纳入
                    if (!fnFilterTest.test(child)) {
                        continue;
                    }
                    // 走到这里，即迭代器不为null，且内有通过test的元素
                    Node<T> n = fnNewNode.newNode(this, currentDepth + 1, child, node);
                    tempNodeRefReusableList.add(n);
                    this.nodeCount += 1;
                }
                if (!tempNodeRefReusableList.isEmpty()) {
                    List<Node<T>> childCollector = listFactory.get();
                    tempNodeRefReusableList.stream().sorted(fnNSort).forEach(childCollector::add);
                    // 向下递归
                    buildingTree(childCollector, listFactory, fnGetChild, fnFilterTest,
                            tempNodeRefReusableList,
                            fnNSort,
                            maxDepth - 1,
                            currentDepth + 1);
                    fnSetChild.set(node, childCollector);
                }
            }

        }
    }

    /**
     * 递归构造显示树的字符串（从根节点直到叶子节点）<br>
     * 若树为空树，则返回的字符串仅包含{@code /}<br>
     * <pre>
     *     {@code
     *     List<Line<Integer>> lines = List.of(
     *             Line.of(1, 2),
     *             Line.of(2, 3),
     *             Line.of(3, 4),
     *             Line.of(1, 5),
     *             Line.of(5, 6)
     *     );
     *     Tree<Integer> tree = Tree.tryOfLines(lines).unwrap();
     *     System.out.println(tree.displayString());
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
     * @see #displayString(int, Function)
     */
    public String displayString() {
        return displayString(depth(), n -> Objects.toString(n.data()));
    }

    /**
     * 递归构造显示树的字符串<br>
     * <pre>
     *     {@code
     *     Tree<T> tree = ...;
     *     String defaultDisplay = tree.displayString();
     *     String customDisplay = tree.displayString(tree.depth(), n -> n.data().toString());
     *     Assert.eq(defaultDisplay, customDisplay);
     *     }
     * </pre>
     *
     * @param displayDepth     要显示的截止深度（包含）
     * @param fnNodeDisplayFmt 函数-表示node显示为字符串的形式
     * @return 显示树的字符串
     * @throws IllegalArgumentException 当给定的截止深度小于-1时
     * @throws NullPointerException     给定的函数为空时
     * @see #displayString()
     */
    public String displayString(int displayDepth,
                                Function<? super Node<T>, ? extends CharSequence> fnNodeDisplayFmt) {
        Objects.requireNonNull(fnNodeDisplayFmt, "fnNodeDisplayFmt is null");
        Err.realIf(displayDepth < -1, IllegalArgumentException::new, "displayDepth < -1");
        if (displayDepth == -1 || isEmpty()) return Const.String.SLASH; // -1 display depth and empty tree display :"/"
        StringBuilder sb = new StringBuilder();
        for (Node<T> root : this.roots) {
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
    private void recursiveBuildDisplayString(Node<T> node, StringBuilder sb, String indent,
                                             Function<? super Node<T>, ? extends CharSequence> fnNodeDisplayFmt,
                                             boolean isLast, int displayDepth) {
        if (displayDepth < 0) return;
        sb.append(indent);
        if (isLast) {
            sb.append("└─ ");
            indent += "   ";
        } else {
            sb.append("├─ ");
            indent += "│  ";
        }
        sb.append(fnNodeDisplayFmt.apply(node)).append(Const.String.LF);
        if (node.isLeaf()) return;
        List<Node<T>> children = this.fnUnsafeGetChild.getNullableChild(node);
        for (int i = 0; i < children.size(); i++) {
            recursiveBuildDisplayString(children.get(i), sb, indent,
                    fnNodeDisplayFmt,
                    i == children.size() - 1, displayDepth - 1);
        }
    }


    /**
     * 树中的节点类型
     */
    public enum NodeType {
        /**
         * 双向节点 （含有对父节点的引用）
         */
        bidirectionalNode,
        /**
         * 单向节点
         */
        unidirectionalNode,
        ;

    }

    /**
     * 树的节点<br>
     *
     * @param <T> 节点载荷类型
     * @implSpec 注意，该类型因为可能带有对父节点的引用 {@link #parentNode()}，
     * 以及节点中含有的从当前节点直到叶子节点的一连串引用 {@link #tryChildNodes()} ，
     * 遂该类型不应当实现/重写 {@code equals},{@code hashcode} 方法，
     * 否则，在调用 {@code equals} 和 {@code hashcode} 这两个方法时将导致无限递归（因为父节点又指向自己的循环引用）从而造成栈内存溢出，
     * 即使该类型的实现类为{@link UnidirectionalNode}（没有对父节点的引用），
     * 也因为直到叶子节点的一连串引用， {@code equals} 和 {@code hashcode} 方法也会是低效的。
     * 遂该类型的 {@code node1.equals(node2)} 语义就是 {@code node1 == node2}
     * @see UnidirectionalNode
     * @see BidirectionalNode
     */
    public sealed interface Node<T> permits BidirectionalNode, UnidirectionalNode {

        /**
         * 当前节点类型
         *
         * @return 节点类型
         */
        NodeType type();

        /**
         * 当前节点深度（边数计算法）
         *
         * @return 当前节点深度
         */
        int depth();

        /**
         * 节点载荷
         *
         * @return 节点载荷
         */
        T data();

        /**
         * 父节点<br>
         * 当前实例为{@link UnidirectionalNode}时，该方法一定返回{@link Optional#empty()}，
         * 当前实例为{@link BidirectionalNode}时，根节点将返回{@link Optional#empty()}
         * <pre>
         *     {@code
         *     if (tree.depth() == 0)
         *         Assert.eq(tree.parentNode(), Optional.empty());
         *     if (tree.nodeType() == NodeType.unidirectionalNode)
         *         Assert.eq(tree.parentNode(), Optional.empty());
         *     }
         * </pre>
         *
         * @return Optional.empty | Some(Node)
         * @see #depth()
         * @see #nodeType()
         */
        Optional<Node<T>> parentNode();

        /**
         * 是否为叶子节点<br>
         * 叶子节点一定没有子节点
         *
         * @return true 是，反之则不是
         * @see #childNodes()
         */
        boolean isLeaf();

        /**
         * 是否为根节点<br>
         * 根节点一定没有父节点
         *
         * @return true 是，反之则不是
         */
        default boolean isRoot() {
            return depth() == 0;
        }

        /**
         * 返回子节点<br>
         * 若当前节点无子节点，调用该方法将抛出异常
         * <pre>
         *     {@code
         *     boolean isLeaf = tree.isLeaf();
         *     if (isLeaf) {
         *         Assert.assertThrows(NoSuchElementException.class, tree::childNodes)
         *     } else {
         *         Assert.assertDoesNotThrow(tree::childNodes)
         *     }
         *     }
         * </pre>
         *
         * @return 子节点
         * @throws NoSuchElementException 当没有子节点时
         * @see #isLeaf()
         * @see #tryChildNodes()
         */
        List<Node<T>> childNodes() throws NoSuchElementException;

        /**
         * 返回子节点<br>
         * 节点有子时返回的Optional内的List一定不为空集合，节点无子时返回{@link Optional#empty()}<br>
         * 返回的List根据构造Tree时给定的ListFactory不同而不同
         * <pre>
         *     {@code
         *     boolean isLeaf = tree.isLeaf();
         *     if (isLeaf) {
         *         Assert.eq(tree.tryChildNodes(), Optional.empty());
         *     } else {
         *         Assert.isTrue(tree.tryChildNodes().isPresent());
         *         Assert.isFalse(tree.tryChildNodes().isEmpty());
         *     }
         *     }
         * </pre>
         *
         * @return Optional.empty | some(NotEmptyList)
         */
        Optional<List<Node<T>>> tryChildNodes();
    }

    /**
     * 双向节点，内{@link #parentNode}为对父节点的引用，若父节点不为{@code null}，
     * 则一定有{@code this.parentNode.childNodes.contains(this) == true}
     *
     * @param <T> 节点载荷
     */
    static final class BidirectionalNode<T> implements Node<T> {
        private int depth;
        private T data;
        private Node<T> parentNode;
        private List<Node<T>> childNodes;

        BidirectionalNode(Tree<T> owner, int depth, T data, Node<T> parentNode) {
            this.depth = depth;
            this.data = data;
            this.parentNode = parentNode;
        }

        @Override
        public String toString() {
            return "BidirectionalNode{" + "data=" + data + ", depth=" + depth + '}';
        }

        @Override
        public NodeType type() {
            return NodeType.bidirectionalNode;
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
        public Optional<Node<T>> parentNode() {
            // can't use Opt.of because root.parent is null
            return Optional.ofNullable(parentNode);
        }

        @Override
        public boolean isLeaf() {
            return childNodes == null
                   || childNodes.isEmpty(); // 构造后默认不会有 empty的ChildNode，除非被变了
        }

        @Override
        public List<Node<T>> childNodes() throws NoSuchElementException {
            Err.realIf(isLeaf(), NoSuchElementException::new, "not found childNodes");
            // 这里可能会有问题：使用isLeaf判定是否返回，虽说初始构建树后不会有empty的List，
            // 但调用方可获取List引用并删除元素或clear，当该List为empty时，
            // 方法将不会返回这个空的List了，可能会有问题，但先暂定如此
            return Collections.unmodifiableList(childNodes); // 返回不可变List包裹
        }

        @Override
        public Optional<List<Node<T>>> tryChildNodes() {
            // 这里可能会有问题：使用isLeaf判定是否返回，虽说初始构建树后不会有empty的List，
            // 但调用方可获取List引用并删除元素或clear，当该List为empty时，
            // 方法将不会返回这个空的List了，可能会有问题，但先暂定如此
            return isLeaf() ? Optional.empty() : Optional.of(Collections.unmodifiableList(childNodes));
            // 返回不可变List包裹
        }

    }

    /**
     * 单向节点，该节点内没有对父节点的引用，只有对子节点的引用
     *
     * @param <T> 节点载荷
     */
    static final class UnidirectionalNode<T> implements Node<T> {
        private int depth;
        private T data;
        private List<Node<T>> childNodes;

        UnidirectionalNode(Tree<T> owner, int depth, T data, Node<T> ignore) {
            this.depth = depth;
            this.data = data;
        }

        @Override
        public String toString() {
            return "UnidirectionalNode{" + "data=" + data + ", depth=" + depth + '}';
        }

        @Override
        public NodeType type() {
            return NodeType.unidirectionalNode;
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
        public Optional<Node<T>> parentNode() {
            return Optional.empty();
        }

        @Override
        public boolean isLeaf() {
            return childNodes == null
                   || childNodes.isEmpty(); // 构造后默认不会有 empty的ChildNode，除非被变了
        }

        @Override
        public List<Node<T>> childNodes() throws NoSuchElementException {
            Err.realIf(isLeaf(), NoSuchElementException::new, "not found childNodes");
            // 这里可能会有问题：使用isLeaf判定是否返回，虽说初始构建树后不会有empty的List，
            // 但调用方可获取List引用并删除元素或clear，当该List为empty时，
            // 方法将不会返回这个空的List了，可能会有问题，但先暂定如此
            return Collections.unmodifiableList(childNodes); // 返回不可变List包裹
        }

        @Override
        public Optional<List<Node<T>>> tryChildNodes() {
            // 这里可能会有问题：使用isLeaf判定是否返回，虽说初始构建树后不会有empty的List，
            // 但调用方可获取List引用并删除元素或clear，当该List为empty时，
            // 方法将不会返回这个空的List了，可能会有问题，但先暂定如此
            return isLeaf() ? Optional.empty() : Optional.of(Collections.unmodifiableList(childNodes));
            // 返回不可变List包裹
        }
    }


    /*
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
