package io.github.baifangkual.bfk.j.mod.core.util;

import io.github.baifangkual.bfk.j.mod.core.model.Line;
import io.github.baifangkual.bfk.j.mod.core.panic.Err;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 工具类，提供操作{@link Line}实体的相关方法
 *
 * @author baifangkual
 * @since 2024/6/27
 */
public class Lines {

    private Lines() {
        throw new UnsupportedOperationException("util class...");
    }

    /**
     * 将给定的pLines集合中每个Line对象的载荷通过给定的map函数转为另一种类型，返回的List为unmodifiedList
     *
     * @param pLines    需转换的Line的集合
     * @param bef2AftFn 转换函数
     * @param <BEF>     Line对象转换后的类型
     * @param <AFT>     Line对象转换前的类型
     * @return 已转换的Line集合
     */
    public static <BEF, AFT> List<Line<AFT>> map(Collection<Line<BEF>> pLines,
                                                 Function<? super BEF, ? extends AFT> bef2AftFn) {
        Objects.requireNonNull(bef2AftFn);
        return Objects.requireNonNull(pLines).stream()
                .map(l -> Line.of(bef2AftFn.apply(l.begin()), bef2AftFn.apply(l.end())))
                .toList();
    }

    /**
     * 给定两个集合，lines 和 all，该方法检测lines能否组成一个或多个有向无环图(directed acyclic graph),
     * 换种说法，该方法检测通过lines构成关系的多个point，能否从头遍历至尾部，也即该point的遍历执行是否是有穷的，
     * lines集合中为多个有向线段（Line对象），描述了all中多个点之间的有向关系，这些关系能够构成n个图数据结构，
     * all集合中则应当有lines集合描述的图结构的所有point实体。
     * 经测试，已知该方法认为能够有穷的（即正确的）结构有如下：<br>
     * * lines 为空列表，即point之间没有关系（即所有point都为孤立点）（这种情况下，所有都为头节点）<br>
     * * lines组成了一个图，并且用了all中有所的point（即all中所有point都在line构成的图的关系中）<br>
     * * lines组成了一个图，并且all中有lines中没有参与的Points（即有孤立点）<br>
     * * lines组成了多个图，并且用了all中所有的Point<br>
     * * lines组成了多个图，但没有用all中所有点（即有孤立点）<br>
     * <p>
     * 20240628 该方法原定在某些情况下显式抛出 IllegalArgumentException，但由于该方法返回值已经表示了检查的lines和
     * points的性质，遂由异常转为方法返回false值
     *
     * @param lines 包含 begin -> end 的结构，有向线段
     * @param all   所有参与line的 点(point)，该集合中允许n个点不参与lines中的关系描述，该给定的集合中Point不应重复
     * @param <P>   line对象 begin -> end 的载荷，应为实现 java.equals & hashcode 的 类型
     * @return true 是一个dag图，也即图能够从头走到尾部， false 不是dag图，（图中有循环|lines中使用了all中未声明的点...)
     */
    public static <P> boolean isDirectedAcyclicGraph(Collection<Line<P>> lines,
                                                     Collection<P> all) {
        Objects.requireNonNull(all);
        Objects.requireNonNull(lines);
        // tod 可能有node不参与图，所以方法需测试
        // fix me 应注意可能的情况：两个Dag，即线连接会构成不止一个图，需注意该情况 fixed
        // 是否有环存在 是否为dag图 使用@拓扑排序算法
        // 图中节点的 入度, 初始化全为0
        Map<P, Integer> inDegree = all.stream()
                //.distinct() // fix IllegalStateException: Duplicate toMap时key相同 该问题直接抛出异常，不处理其
                .collect(Collectors.toMap(p -> p, p -> 0));
        for (Line<P> line : lines) {
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
            for (Line<P> l : lines) {
                P begin = l.begin();
                P end = l.end();
                if (begin.equals(point)) {
                    Integer newInDegree = inDegree.computeIfPresent(end, (k, inDer) -> inDer - 1);
                    if (newInDegree != null && newInDegree.equals(0)) que.add(end);
                }
            }
        }
        // 当拓扑排序完成后，检查result和fullPoint大小，若不想等，则说明图中出现循环，不是dag图
        return r == all.size();
    }

    /**
     * 给定一个lines集合和一个all可迭代对象，返回lines（有向图）的头节点（开始节点）,
     * 若找不到或没有头节点，则返回的Set为empty
     * <p>
     * 20240628 该方法原定在某些情况下显式抛出 IllegalETLNodeConfigStateException，
     * 但由于该方法返回值已经表示了检查的lines和points的性质，遂由异常转为方法返回set::empty值
     *
     * @param lines 描述了point的关系，Line为有向的，从 begin -> end
     * @param all   一组point，point为lines中Line的载荷对象，参与了lines描述的关系
     * @param <P>   line对象 begin -> end 的载荷，应为实现 java.equals & hashcode 的 类型
     * @return 头节点（开始节点）,若找不到头节点，则Set为empty
     */
    public static <P> Set<P> findHeaderPoints(Collection<Line<P>> lines,
                                              Iterable<P> all) {
        Objects.requireNonNull(all);
        Objects.requireNonNull(lines);
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
     * 给定 nullable 头节点集合， 所有节点集合，和节点关系lines，返回可遍历完所有节点且按照lines关系顺序获取的队列，
     * Queue[Set[Point]] 当中Set[Point] 表示这个Set内的Point可同时被获取到（多线程下...等情况...),
     * 当给定的头节点集合为null时，将在该方法内计算头节点集合，孤立点将被直接当作头节点,
     * 该方法可保证所有Point能被遍历且仅遍历一次
     *
     * @param headerPoints 头节点集合
     * @param all          所有点集合
     * @param lines        点的关系，有向
     * @param <p>          载荷实体，应当实现 java.equals & hashcode 方法
     * @return 可遍历完所有点的按照lines所指定的关系的队列
     */
    public static <p> LinkedList<Set<p>> forOrder(Collection<p> all,
                                                  Collection<Line<p>> lines,
                                                  Collection<p> headerPoints) {
        Objects.requireNonNull(all);
        Err.realIf(headerPoints != null && headerPoints.isEmpty(), IllegalArgumentException::new,
                "给定参数认定无头节点，遂无法从头排序");
        final LinkedList<Set<p>> midResultNextIds = new LinkedList<>();
        if (lines == null || lines.isEmpty()) { // 因为没有lines 为确保执行，所有都为头节点即可
            midResultNextIds.add(new HashSet<>(all));
            return midResultNextIds;
        }
        // 容错，若不传递headerPoints 则在此处计算之
        if (headerPoints == null) {
            headerPoints = findHeaderPoints(lines, all);
        }
        // 循环nextIds poll 为 null 表示 已到终点
        Queue<Set<p>> loopNextIdsQueue = new LinkedList<>();
        // 存放执行顺序，但有重复执行的，所以该为中间结果，后续从后到前遍历去除重复
        // init headerIds
        // tod 可能有不参与线的 孤立点, fix 20240628 孤立点被当作头节点，从头节点遍历
        loopNextIdsQueue.add(new HashSet<>(headerPoints)); //immutable ...
        // init full mut,on end must empty
        Set<p> lineMutFullPoint = new HashSet<>(all);
        // 从lines复制其引用，然后可变 每次找到可用的从list中删除
        List<Line<p>> mutLines = new ArrayList<>(lines);
        while (!loopNextIdsQueue.isEmpty()) {
            Set<p> next = loopNextIdsQueue.poll();
            midResultNextIds.add(next); // 每次将 loopNextIdsQueue中构成的执行顺序的set引用copy至此
            Set<p> currentNextIds = new HashSet<>();
            for (p nId : next) {
                lineMutFullPoint.remove(nId);
                for (Line<p> line : mutLines) {
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
            if (!currentNextIds.isEmpty()) loopNextIdsQueue.add(currentNextIds);
        }
        // check lineMutFullPoint, 当点都被走到时，这个集合应当为null，否则说明其为孤立点，
        // fix 是否上述已有逻辑完成该？ ...
        Err.realIf(!lineMutFullPoint.isEmpty(), IllegalStateException::new,
                "以 {} 作为头节点，这些Point将无法被遍历到:Points: {}", headerPoints, lineMutFullPoint);
        // 至此 midResultNextIds中已经排序执行顺序了，但还有重复执行的，需将其从后往前遍历，找到需执行的实际顺序
        for (int i = midResultNextIds.size() - 1; i >= 0; i--) {
            Set<p> endIds = midResultNextIds.get(i);
            for (int insIdx = i - 1; insIdx >= 0; insIdx--) {
                Set<p> beginIds = midResultNextIds.get(insIdx);
                beginIds.removeAll(endIds);
            }
        }
        return midResultNextIds;
    }


    /**
     * see doc {@link Lines#forOrder(Collection, Collection, Collection)}
     *
     * @param all   所有点集合
     * @param lines 点的关系，有向
     * @param <P>   载荷实体，应当实现 java.equals & hashcode 方法
     * @return 可遍历完所有点的按照lines所指定的关系的队列
     */
    public static <P> LinkedList<Set<P>> forOrder(Collection<P> all,
                                                  Collection<Line<P>> lines) {
        return forOrder(all, lines, null);
    }


}
