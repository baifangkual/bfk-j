package io.github.baifangkual.bfk.j.mod.core.lang;

import io.github.baifangkual.bfk.j.mod.core.mark.Iter;
import io.github.baifangkual.bfk.j.mod.core.panic.Err;
import io.github.baifangkual.bfk.j.mod.core.trait.Cloneable;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <b>有向线</b><br>
 * 对象，表示两个实体之间的关系，两个实体一个为begin，一个为end，两个实体均不允许为null
 *
 * @author baifangkual
 * @since 2024/6/18 v0.0.4
 */
@SuppressWarnings("ClassCanBeRecord")
public class Line<P> implements Iter<P>, Serializable, Cloneable<Line<P>> {

    @Serial
    private static final long serialVersionUID = 1L;

    private final P begin;
    private final P end;

    private Line(P begin, P end) {
        Objects.requireNonNull(begin, "begin is null");
        Objects.requireNonNull(end, "end is null");
        this.begin = begin;
        this.end = end;
    }

    /**
     * 创建“有向线”对象<br>
     * 给定的两个对象不允许为空，否则将抛出异常
     *
     * @param begin 线段起点
     * @param end   线段终点
     * @param <P>   点类型参数
     * @return 有向线对象
     * @throws NullPointerException 当给定的两个对象中有null时
     */
    public static <P> Line<P> of(P begin, P end) {
        return new Line<>(begin, end);
    }

    /**
     * 返回起点对象
     *
     * @return 起点对象
     */
    public P begin() {
        return begin;
    }

    /**
     * 返回终点对象
     *
     * @return 终点对象
     */
    public P end() {
        return end;
    }

    /**
     * 根据给定的函数转换起点和终点实体为新类型实体并返回新的{@link Line}实体<br>
     * 给定的转换函数不允许为空，否则抛出异常
     *
     * @param fn  转换函数
     * @param <B> 新实体类型
     * @return 载荷新类型实体的新有向线对象
     * @throws NullPointerException 当给定的转换函数为空时
     */
    public <B> Line<B> map(Function<? super P, ? extends B> fn) {
        Objects.requireNonNull(fn, "fn is null");
        return Line.of(fn.apply(begin), fn.apply(end));
    }

    /**
     * 反转线段方向<br>
     * 该方法返回新有向线对象
     *
     * @return 反转后的新有向线对象
     */
    public Line<P> reverse() {
        return Line.of(end, begin);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Line<?> line)) return false;
        return Objects.equals(begin, line.begin) && Objects.equals(end, line.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(begin, end);
    }

    @Override
    public String toString() {
        return "Line[(" + begin + ") -> (" + end + ")]";
    }


    // static fn ----------------------------------------------------------------------

    /**
     * 将给定的可迭代对象中每个Line对象的载荷通过给定的map函数转为另一种类型<br>
     * 返回的List为unmodifiedList
     *
     * @param lines 需转换的n个Line
     * @param fn    转换函数
     * @param <BP>  Line对象 begin 和 end 转换前的类型
     * @param <AP>  Line对象 begin 和 end 转换后的类型
     * @return 已转换的Line集合（unmodifiedList）
     * @throws NullPointerException 当给定的可迭代对象为空时，或给定的转换函数为空时
     */
    public static <BP, AP> List<Line<AP>> map(Iterable<Line<BP>> lines,
                                              Function<? super BP, ? extends AP> fn) {
        Objects.requireNonNull(fn, "fn is null");
        return Iter.toStream(lines)
                .map(l -> of(fn.apply(l.begin()), fn.apply(l.end())))
                .toList();
    }

    /**
     * 给定两个可迭代对象，lines 和 all，该方法检测lines能否组成一个或多个有向无环图(directed acyclic graph)<br>
     * 换种说法，该方法检测通过lines构成关系的多个{@link P}，能否从头遍历至尾部，也即该{@link P}的遍历执行是否是有穷的，
     * lines集合中为多个有向线段（Line对象），描述了all中多个点之间的有向关系，这些关系能够构成n个图数据结构，
     * all集合中则应当有lines集合描述的图结构的所有{@link P}实体。
     * 经测试，已知该方法认为能够有穷的（即正确的）结构有如下：<br>
     * <ul>
     *     <li>lines 为空列表，即{@link P}之间没有关系（即所有{@link P}都为孤立点）（这种情况下，所有都为头节点）</li>
     *     <li>lines组成了一个图，并且用了all中有所的{@link P}（即all中所有{@link P}都在line构成的图的关系中）</li>
     *     <li>lines组成了一个图，并且all中有lines中没有参与的{@link P}s（即有孤立点）</li>
     *     <li>lines组成了多个图，并且用了all中所有的{@link P}</li>
     *     <li>lines组成了多个图，但没有用all中所有点（即有孤立点）</li>
     * </ul>
     *
     * @param lines 包含n个 begin -> end 的结构，有向线段
     * @param all   所有参与line的 点({@link P})，该集合中允许n个点不参与lines中的关系描述，该给定的集合中{@link P}不应重复
     * @param <P>   节点类型（line对象 begin 和 end 的类型）
     * @return true 是一个dag图，也即图能够从头走到尾部， false 不是dag图，（图中有循环|lines中使用了all中未声明的点...)
     * @throws NullPointerException  当给定的lines为空或all为空时
     * @throws IllegalStateException 当给定的all中没有任何一个节点时
     * @apiNote 该方法内会使用P类型做MapKey来计算出度和入度，遂P类型是否实现equals和hashcode应在业务侧做考量
     */
    public static <P> boolean isDirectedAcyclicGraph(Iterable<Line<P>> lines,
                                                     Iterable<P> all) {
        // tod 可能有node不参与图，所以方法需测试
        // fix me 应注意可能的情况：两个Dag，即线连接会构成不止一个图，需注意该情况 fixed
        // 是否有环存在 是否为dag图 使用@拓扑排序算法
        // 图中节点的 入度, 初始化全为0
        List<P> findAllPoint = Iter.toStream(all).toList();
        List<Line<P>> findAllLines = Iter.toStream(lines).toList();
        Map<P, Integer> inDegree = findAllPoint.stream()
                //.distinct() // fix IllegalStateException: Duplicate toMap时key相同 该问题直接抛出异常，不处理其
                .collect(Collectors.toMap(p -> p, p -> 0));
        for (Line<P> line : findAllLines) {
            P end = line.end();
            Integer pointInDegree = inDegree.computeIfPresent(end, (k, inDer) -> inDer + 1);
            if (pointInDegree == null) return false;
            //throw new IllegalArgumentException(StrFmt
            //   .fmt("在lines中发现了all中未包含的点，非法参数"));
        }
        Queue<P> que = new LinkedList<>(); // //每次从queue中取， queue中全为入度为0的
        int r = 0; //被遍历便 + 1
        for (Map.Entry<P, Integer> ent : inDegree.entrySet()) {
            if (ent.getValue().equals(0)) {
                que.add(ent.getKey());
            }
        }
        // 执行拓扑排序
        while (!que.isEmpty()) {
            P point = que.poll();
            r += 1;
            for (Line<P> l : findAllLines) {
                P begin = l.begin();
                P end = l.end();
                if (begin.equals(point)) {
                    Integer newInDegree = inDegree.computeIfPresent(end, (k, inDer) -> inDer - 1);
                    if (newInDegree != null && newInDegree.equals(0)) que.add(end);
                }
            }
        }
        // 当拓扑排序完成后，检查result和fullPoint大小，若不相等，则说明图中出现循环，不是dag图
        return r == findAllPoint.size();
    }

    /**
     * 检测给定的多个线对象能否组成一个或多个有向无环图(directed acyclic graph)<br>
     * 若可迭代对象内
     *
     * @param lines n个线段对象，表示节点关系
     * @param <P>   节点数据类型
     * @return true 可以构成dag图，反之则不可以
     * @throws NullPointerException 当给定的可迭代对象为空时
     * @see #isDirectedAcyclicGraph(Iterable, Iterable)
     */
    public static <P> boolean isDirectedAcyclicGraph(Iterable<Line<P>> lines) {
        Objects.requireNonNull(lines, "lines is null");
        return isDirectedAcyclicGraph(lines, findAllNode(lines));
    }

    /**
     * 给定可迭代对象 lines 和 all ，返回lines（n个有向图）的n个头节点（开始节点）<br>
     * 头节点定义：一定不为所有{@link Line#end()}指向的节点，可能为{@link Line#begin()}指向的节点(如果在lines中的话)，
     * 若找不到或没有头节点，则返回的Set为empty
     *
     * @param lines 包含n个 begin -> end 的结构，有向线段
     * @param all   所有参与line的 点({@link P})，该集合中允许n个点不参与lines中的关系描述，该给定的集合中{@link P}不应重复
     * @param <P>   节点类型（line对象 begin 和 end 的类型）
     * @return n个头节点（开始节点）,若找不到头节点，则Set为empty
     * @throws NullPointerException 当给定的lines为空或all为空时
     * @apiNote 该方法内使用HashSet做差集，P类型是否实现equals和hashcode应在业务侧做考量，另外，孤立点（仅在all中而不在lines中出现的点）
     * 也被认为是头节点
     */
    public static <P> Set<P> findHeaderNodes(Iterable<Line<P>> lines, Iterable<P> all) {
        Objects.requireNonNull(all, "all is null");
        Objects.requireNonNull(lines, "lines is null");
        // mut result 可变集合，不可操作入参集合
        final Set<P> headers = new HashSet<>();
        // 先向headerSet中添加所有，以确保孤立点能够被当作头节点
        all.forEach(headers::add);
        final Set<P> ends = new HashSet<>();
        for (Line<P> line : lines) {
            headers.add(line.begin());
            ends.add(line.end());
        }
        headers.removeAll(ends);
        return headers;
    }

    /**
     * 给定包含多个Line的可迭代对象，通过这些Line的begin和end节点找到所有参与关系的节点（P）
     *
     * @param lines 节点关系，n个有向线段
     * @param <P>   节点类型
     * @return 所有关系的节点
     * @throws NullPointerException 当给定的lines为空时
     * @apiNote 该方法内使用HashSet，P类型是否实现equals和hashcode应在业务侧做考量
     */
    public static <P> Set<P> findAllNode(Iterable<Line<P>> lines) {
        Objects.requireNonNull(lines, "lines is null");
        final Set<P> points = new HashSet<>();
        for (Line<P> l : lines) {
            for (P point : l) {
                points.add(point);
            }
        }
        return points;
    }

    /**
     * 给定 节点关系lines、所有节点、头节点集合，返回可从头节点遍历完所有节点且按照lines关系顺序获取的队列<br>
     * 返回的 LinkedList[List[Point]] 当中List[Point] 表示这个List内的Point可同时被获取到/为平行关系（多线程下...等情况...),
     * 孤立点将被直接当作头节点,该方法可保证所有Point能被遍历且仅遍历一次
     *
     * @param headers 头节点集合
     * @param all     所有节点
     * @param lines   节点关系，有向
     * @param <P>     节点类型（line对象 begin 和 end 的类型）
     * @return 可遍历完所有点的按照lines所指定的关系的队列
     * @throws NullPointerException     当给定的lines、all、headers任意一个为空时
     * @throws IllegalArgumentException 当给定的headers可迭代对象中没有元素时，即认定无头节点时
     * @throws IllegalStateException    当给定的节点中有些节点无法从头节点被遍历到时
     * @apiNote 该方法内使用HashSet等做各种比较，遂P类型是否实现equals和hashcode应在业务侧做考量，另外，孤立点（仅在all中而不在lines中出现的点）
     * 也被认为是头节点
     */
    public static <P> LinkedList<List<P>> orderDAGQueue(Iterable<Line<P>> lines,
                                                   Iterable<P> all,
                                                   Iterable<P> headers) {
        List<Line<P>> findLines = Iter.toStream(lines).toList();
        List<P> findAll = Iter.toStream(all).toList();
        List<P> findHeaders = Iter.toStream(headers).toList();
        // 当headers迭代器不为空，且headerList为空，即认为明确表示没有头节点，该情况应直接抛出异常
        Err.realIf(findHeaders.isEmpty(), IllegalArgumentException::new,
                "给定参数认定无头节点，遂无法从头排序");
        final LinkedList<List<P>> midResultNextIds = new LinkedList<>();
        if (findLines.isEmpty()) { // 因为没有lines 为确保执行，所有都为头节点即可
            midResultNextIds.add(new ArrayList<>(findAll));
            return midResultNextIds;
        }
        // 循环nextIds poll 为 null 表示 已到终点
        Queue<List<P>> loopNextIdsQueue = new LinkedList<>();
        // 存放执行顺序，但有重复执行的，所以该为中间结果，后续从后到前遍历去除重复
        // init headerIds
        // tod 可能有不参与线的 孤立点, fix 20240628 孤立点被当作头节点，从头节点遍历
        loopNextIdsQueue.add(new ArrayList<>(findHeaders)); //immutable ...
        // init full mut,on end must empty
        List<P> lineMutFullPoint = new ArrayList<>(findAll);
        // 从lines复制其引用，然后可变 每次找到可用的从list中删除
        List<Line<P>> mutLines = new ArrayList<>(findLines);
        // 重复利用该Set临时存储 next
        Set<P> currentNextIds = new HashSet<>();
        while (!loopNextIdsQueue.isEmpty()) {
            List<P> next = loopNextIdsQueue.poll();
            midResultNextIds.add(next); // 每次将 loopNextIdsQueue中构成的执行顺序的set引用copy至此
            // 每次清理该
            currentNextIds.clear();
            for (P nId : next) {
                lineMutFullPoint.remove(nId);
                for (Line<P> line : mutLines) {
                    if (line.begin().equals(nId)) { // 找到以自己开始的
                        currentNextIds.add(line.end()); //将自己的next放入set中（防止重复）
                        // fix me 20240523 被遍历到的若是删除，会影响dag执行顺序的构成
                        //  原因是因为 被首先遍历到的line，后续遍历时仍可能会被使用
                        //  例如 a -> b -> c 和 a -> e -> b -> c 同时存在这两条线
                        //  但是因为第二批遍历到 b 时，删除了b -> c的线，则后续第三批 b 的线便找不到了
                        // iterLines.remove(); //从mutLines中删除已迭代的，减少迭代次数
                    }
                }
            }
            // 到此，mutLines中 current 的所有nextId已经放入 currentNextIds 中, 可能为empty，因为若当前为最后一个时...
            if (!currentNextIds.isEmpty()) loopNextIdsQueue.add(new ArrayList<>(currentNextIds));
        }
        // check lineMutFullPoint, 当点都被走到时，这个集合应当为null，否则说明其为孤立点，
        // fix 是否上述已有逻辑完成该？ ...
        Err.realIf(!lineMutFullPoint.isEmpty(), IllegalStateException::new,
                "以 {} 作为头节点，这些节点将无法被遍历到: {}", findHeaders, lineMutFullPoint);
        // 至此 midResultNextIds中已经排序执行顺序了，但还有重复执行的，需将其从后往前遍历，找到需执行的实际顺序
        for (int i = midResultNextIds.size() - 1; i >= 0; i--) {
            List<P> endIds = midResultNextIds.get(i);
            for (int insIdx = i - 1; insIdx >= 0; insIdx--) {
                List<P> beginIds = midResultNextIds.get(insIdx);
                beginIds.removeAll(endIds);
            }
        }
        return midResultNextIds;
    }


    /**
     * 给定 节点关系lines、所有节点，返回可从头节点遍历完所有节点且按照lines关系顺序获取的队列<br>
     * 返回的 LinkedList[List[Point]] 当中List[Point] 表示这个List内的Point可同时被获取到/为平行关系（多线程下...等情况...),
     * 孤立点将被直接当作头节点,该方法可保证所有Point能被遍历且仅遍历一次
     *
     * @param all   所有节点
     * @param lines 节点关系，有向
     * @param <P>   节点类型（line对象 begin 和 end 的类型）
     * @return 可遍历完所有点的按照lines所指定的关系的队列
     * @throws NullPointerException     当给定的lines、all、headers任意一个为空时
     * @throws IllegalArgumentException 当给定的headers可迭代对象中没有元素时，即认定无头节点时
     * @throws IllegalStateException    当给定的节点中有些节点无法从头节点被遍历到时
     * @apiNote 该方法内使用HashSet等做各种比较，遂P类型是否实现equals和hashcode应在业务侧做考量，另外，孤立点（仅在all中而不在lines中出现的点）
     * 也被认为是头节点
     * @implNote 该方法内将根据参数lines和all自动寻找头节点
     * @see #orderDAGQueue(Iterable, Iterable, Iterable)
     * @see #findHeaderNodes(Iterable, Iterable)
     */
    public static <P> LinkedList<List<P>> orderDAGQueue(Iterable<Line<P>> lines,
                                                   Iterable<P> all) {
        return orderDAGQueue(lines, all, findHeaderNodes(lines, all));
    }

    /**
     * 给定 节点关系lines、返回可从头节点遍历完所有节点且按照lines关系顺序获取的队列<br>
     * 返回的 LinkedList[List[Point]] 当中List[Point] 表示这个List内的Point可同时被获取到/为平行关系（多线程下...等情况...),
     * 孤立点将被直接当作头节点,该方法可保证所有Point能被遍历且仅遍历一次
     *
     * @param lines 节点关系，有向
     * @param <P>   节点类型（line对象 begin 和 end 的类型）
     * @return 可遍历完所有点的按照lines所指定的关系的队列 | RuntimeException
     * @throws NullPointerException     当给定的lines、all、headers任意一个为空时
     * @throws IllegalArgumentException 当给定的headers可迭代对象中没有元素时，即认定无头节点时
     * @throws IllegalStateException    当给定的节点中有些节点无法从头节点被遍历到时，或者给定的节点关系无法构成一个DAG图时
     * @apiNote P类型是否实现equals和hashcode应在业务侧做考量
     * @implNote 该方法内将根据参数lines自动寻找参与的所有节点和头节点
     * @see #orderDAGQueue(Iterable, Iterable)
     * @see #isDirectedAcyclicGraph(Iterable, Iterable)
     * @see #findHeaderNodes(Iterable, Iterable)
     * @see #findAllNode(Iterable)
     */
    public static <P> R<LinkedList<List<P>>, RuntimeException> orderDAGQueue(Iterable<Line<P>> lines) {
        return R.ofSupplier(() -> {
            Set<P> all = findAllNode(lines);
            Err.realIf(!isDirectedAcyclicGraph(lines, all),
                    IllegalStateException::new, "lines is not a directed acyclic graph");
            return orderDAGQueue(lines, all);
        });
    }

    // static fn ----------------------------------------------------------------------


    /**
     * 返回迭代器<br>
     * 其中固定只有两个元素，该方法主要是方便for-each
     *
     * @return 迭代器
     */
    @Override
    public Iterator<P> iterator() {
        return new Iterator<>() {
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < 2;
            }

            @Override
            public P next() {
                return switch (cursor++) {
                    case 0 -> begin;
                    case 1 -> end;
                    default -> throw new NoSuchElementException("No more!");
                };
            }
        };
    }

    /**
     * 返回载荷两个元素的流
     *
     * @return 流
     */
    @Override
    public Stream<P> stream() {
        return Stream.of(begin, end);
    }

    /**
     * 返回载荷两个元素的并行流
     *
     * @return 并行流
     */
    @Override
    public Stream<P> parallelStream() {
        return Stream.of(begin, end).parallel();
    }

    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public Line<P> clone() {
        return Line.of(begin, end);
    }
}
