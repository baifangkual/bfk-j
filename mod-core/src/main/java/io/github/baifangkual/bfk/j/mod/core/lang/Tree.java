package io.github.baifangkual.bfk.j.mod.core.lang;


import io.github.baifangkual.bfk.j.mod.core.mark.Iter;
import io.github.baifangkual.bfk.j.mod.core.panic.Err;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * <b>多叉树</b><br>
 * 该树的深度 {@link #depth} 使用边数计算法（空树（{@link #roots} is empty）的深度 = -1，只有根节点的树的深度 = 0）<br>
 * 因为该树为二叉树的父集，遂没有中序遍历<br>
 *
 * @author baifangkual
 * @since 2023/8/18 v0.0.6
 */
public final class Tree<T> {

    private final List<Node<T>> roots;
    private final NodeType nodeType;
    private final int maxDepth;
    private final int rootCount;

    // ------- mutable
    private int nodeCount = 0;
    private int depth = -1;
    // ------- mutable

    /**
     * 根节点
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
        return rootCount;
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
     * 树能够承载的最大深度
     *
     * @return 树能够承载的最大深度
     */
    public int maxDepth() {
        return maxDepth;
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
     * 构造方法
     *
     * @param roots              根节点，一个或多个
     * @param fnGetChild         函数，要求给定一个实体，返回这个实体的子实体，返回的迭代器可以为null，也可以没有元素
     * @param nodeType           树中节点使用什么类型的实现（{@link BidirectionalNode} or {@link UnidirectionalNode}）
     * @param fnSort             函数-实体排序的函数
     * @param listFactory        函数-List构造方法引用
     * @param fnPreCheckHasChild 函数(nullable)，要求给定一个实体，返回布尔值标识该实体是否有子，这在lsChildFn过重时是一种优化
     * @param fnPreFilter        函数，预先处理树中实体，要求给定一个实体，当返回的布尔值为false时，表示实体不加入树
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
        final Fn3<T> fnNewNode = switchNBuildFn(nodeType);
        // node set child
        final CS<T> fnSetChild = switchNCSetFn(nodeType);
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
                .map(t -> fnNewNode.newNode(0, t, null))
                .sorted(fnNSort)
                .forEach(rootCollector::add);
        this.maxDepth = maxDepth;
        this.nodeType = nodeType;
        // 允许没有元素的树，若是，则无需下构建树子
        if (maxDepth == -1 || rootCollector.isEmpty()) {
            rootCollector.clear(); // 因为内部的List由ListFactory控制，遂该即使为空树，roots也不应引用Collections.emptyList
            this.roots = rootCollector;
            this.rootCount = 0;
        } else {
            this.nodeCount = rootCollector.size();
            buildingTree(rootCollector, listFactory, fnCompGetChild, fnPreFilter,
                    listFactory.get(), // 复用临时存储直接child的，通常直接child不会太多
                    fnNewNode, fnSetChild, fnNSort,
                    maxDepth - 1, 0);
            this.roots = rootCollector;
            this.rootCount = rootCollector.size();
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
     * @return Ok(Tree) | Err(RuntimeException)
     * @apiNote 若给定的lines为空、其中没有任何元素、线段构成的关系包含循环边、
     * 线段中有实体有一个以上父节点等，返回{@link R.Err}
     * @see #tryToLines(Supplier, Function)
     */
    public static <E> R<Tree<E>, RuntimeException> tryOfLines(Iterable<Line<E>> lines) {
        return R.ofSupplier(() -> {
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
     * @return Ok(Lines) | Err(RuntimeException)
     * @apiNote 仅包含根节点的树（因为没有子节点，无法构造{@link Line})、
     * 给定的listFactory为空、给定的fn为空等，均会返回{@link R.Err}
     * @see #tryOfLines(Iterable)
     */
    public <V> R<List<Line<V>>, RuntimeException> tryToLines(Supplier<? extends List<Line<V>>> listFactory,
                                                             Function<? super Node<T>, ? extends V> fn) {
        return R.ofSupplier(() -> {
            final List<Line<V>> r = listFactory.get();// get list type link? arr?
            final Queue<Node<T>> queue = new LinkedList<>(); //BFS queue
            for (Node<T> root : this.roots) { // 先将root判定，添加到queue中
                if (root.isLeaf()) throw new IllegalStateException("tree just root node, can't build Line.end");
                queue.add(root);
            }
            while (!queue.isEmpty()) {
                Node<T> n = queue.poll();
                List<Node<T>> childNodes = n.childNodes();
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

    // todo impl sub() 分裂 、从root分裂、创建子树
    private Iterator<Node<T>> iterator() {
        return new TreeIter<>(this.roots);
    }
    // todo impl ofNodes...
    // todo impl iter...
    private static class TreeIter<E> implements Iterator<Node<E>> {
        final List<Node<E>> roots;
        Node<E> refCurrent = null;
        TreeIter(List<Node<E>> roots) {
            this.roots = roots;
        }
        @Override
        public boolean hasNext() {
            return refCurrent != null;
        }
        @Override
        public Node<E> next() {
            return null;
        }
        @Override
        public void remove() {
            Iterator.super.remove();
        }
    }


    /**
     * 递归-DFS先序遍历
     *
     * @param node 树节点
     * @param fn   函数
     * @param <T>  节点载荷
     * @throws NullPointerException 给定的节点为空或函数为空时
     */
    private static <T> void dfsPreOrder(Node<T> node, Consumer<? super Node<T>> fn) {
        fn.accept(node); // 先访问
        if (!node.isLeaf()) {
            for (Node<T> child : node.childNodes()) {
                dfsPreOrder(child, fn);// 递归子
            }
        }
    }

    /**
     * 递归-DFS后序遍历
     *
     * @param node 树节点
     * @param fn   函数
     * @param <T>  节点载荷
     * @throws NullPointerException 给定的节点为空或函数为空时
     */
    private static <T> void dfsPostOrder(Node<T> node, Consumer<? super Node<T>> fn) {
        if (!node.isLeaf()) {
            for (Node<T> child : node.childNodes()) {
                dfsPostOrder(child, fn);// 递归子
            }
        }
        fn.accept(node); // 后访问
    }

    /**
     * 非递归-BFS遍历
     *
     * @param fnGetNodes 树节点提供函数，提供n个树节点，这些节点应是同一深度的，
     *                   否则可能会导致一个实体被遍历不止一次
     * @param fn         函数
     * @param <T>        节点载荷
     * @throws NullPointerException 给定的节点为空或函数为空时
     */
    private static <T> void bfs(Supplier<? extends Iterable<Node<T>>> fnGetNodes,
                                Consumer<? super Node<T>> fn) {
        Objects.requireNonNull(fnGetNodes, "fnGetNodes is null");
        Objects.requireNonNull(fn, "fn is null");
        Queue<Node<T>> queue = new LinkedList<>();
        fnGetNodes.get().forEach(queue::add);
        while (!queue.isEmpty()) {
            Node<T> current = queue.poll();
            fn.accept(current); // 处理当前节点
            // 将当前节点的所有子节点加入队列
            if (current.isLeaf()) continue;
            queue.addAll(current.childNodes());
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
            dfsPreOrder(root, fn);
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
            dfsPostOrder(root, fn);
        }
    }


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
    private interface Fn3<T> {
        Node<T> newNode(int dep, T vRef, Node<T> pRef);
    }

    /**
     * gen node new fn by node type
     */
    private Fn3<T>
    switchNBuildFn(NodeType type) {
        return switch (type) {
            case bidirectionalNode -> BidirectionalNode::new;
            case unidirectionalNode -> UnidirectionalNode::new;
        };
    }

    /**
     * node child setting
     */
    @FunctionalInterface
    private interface CS<T> {
        void set(Node<T> self, List<Node<T>> child);
    }

    /**
     * gen node child set fn by node type
     */
    private CS<T>
    switchNCSetFn(NodeType type) {
        return switch (type) {
            case bidirectionalNode -> (self, child) ->
                    ((BidirectionalNode<T>) self).childNodes = child;
            case unidirectionalNode -> (self, child) ->
                    ((UnidirectionalNode<T>) self).childNodes = child;
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
     * 递归构造树
     *
     * @param nodes                   节点
     * @param listFactory             函数-List构造方法引用
     * @param fnGetChild              函数-从实例获取其子
     * @param fnFilterTest            函数-跳过不符合条件的实例
     * @param tempNodeRefReusableList 复用的临时List，存储Node引用
     * @param fnNewNode               函数-Node构造方法引用
     * @param fnSetChild              函数-向Node绑定其子
     * @param fnNSort                 函数-排序Node
     * @param maxDepth                最大停止深度（包含）
     * @param currentDepth            当前深度
     */
    private void buildingTree(List<Node<T>> nodes,
                              Supplier<? extends List<Node<T>>> listFactory,
                              Function<? super T, ? extends Iterable<? extends T>> fnGetChild,
                              Predicate<? super T> fnFilterTest,
                              List<Node<T>> tempNodeRefReusableList,
                              Fn3<T> fnNewNode,
                              CS<T> fnSetChild,
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
                    Node<T> n = fnNewNode.newNode(currentDepth + 1, child, node);
                    tempNodeRefReusableList.add(n);
                    this.nodeCount += 1;
                }
                if (!tempNodeRefReusableList.isEmpty()) {
                    List<Node<T>> childCollector = listFactory.get();
                    tempNodeRefReusableList.stream().sorted(fnNSort).forEach(childCollector::add);
                    // 向下递归
                    buildingTree(childCollector, listFactory, fnGetChild, fnFilterTest,
                            tempNodeRefReusableList,
                            fnNewNode,
                            fnSetChild,
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
        return displayString(depth(), n -> n.data().toString());
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
        Err.realIf(maxDepth < -1, IllegalArgumentException::new, "displayDepth < -1");
        if (displayDepth == -1) return Const.String.SLASH; // empty tree display :"/"
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
        List<Node<T>> children = node.childNodes();
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
    public static final class BidirectionalNode<T> implements Node<T> {
        private final int depth;
        private final T data;
        private final Node<T> parentNode;
        private List<Node<T>> childNodes;

        BidirectionalNode(int depth, T data, Node<T> parentNode) {
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
            return childNodes;
        }

        @Override
        public Optional<List<Node<T>>> tryChildNodes() {
            // 这里可能会有问题：使用isLeaf判定是否返回，虽说初始构建树后不会有empty的List，
            // 但调用方可获取List引用并删除元素或clear，当该List为empty时，
            // 方法将不会返回这个空的List了，可能会有问题，但先暂定如此
            return isLeaf() ? Optional.empty() : Optional.of(childNodes);
        }

    }

    /**
     * 单向节点，该节点内没有对父节点的引用，只有对子节点的引用
     *
     * @param <T> 节点载荷
     */
    public static final class UnidirectionalNode<T> implements Node<T> {
        private final int depth;
        private final T data;
        /**
         * 子节点们 nullable
         */
        private List<Node<T>> childNodes;

        UnidirectionalNode(int depth, T data, Node<T> ignore) {
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
            return childNodes;
        }

        @Override
        public Optional<List<Node<T>>> tryChildNodes() {
            // 这里可能会有问题：使用isLeaf判定是否返回，虽说初始构建树后不会有empty的List，
            // 但调用方可获取List引用并删除元素或clear，当该List为empty时，
            // 方法将不会返回这个空的List了，可能会有问题，但先暂定如此
            return isLeaf() ? Optional.empty() : Optional.of(childNodes);
        }
    }


}
